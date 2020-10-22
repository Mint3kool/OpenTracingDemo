package com.example.openTracing.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import lombok.Data;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

import com.example.openTracing.Consumer;
import com.example.openTracing.Producer;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.okhttp3.TagWrapper;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import java.io.IOException;

import javax.jms.Message;

/**
 * Note: jmsTemplate is used for completely synchronous jms client calls
 */
@Service
@Component
@RestController
@RequestMapping("/api")
public class TracingResource {

	@Autowired
	private Consumer consumer;

	@Autowired
	private Producer producer;

	@Autowired
	private ApplicationContext ctx;

	OkHttpClient client;

//	@Autowired
//	private Tracer tracer;

	private static final Logger logger = LoggerFactory.getLogger(TracingResource.class);

	@RequestMapping(value = "/request/{queue}", method = RequestMethod.GET)
	public String getRequest(@PathVariable("queue") String queue) {
		JmsTemplate jms = ctx.getBean(JmsTemplate.class);
		Object o = jms.receiveAndConvert(queue);
		return o.toString();
	}

	@RequestMapping(value = "/batch/{queue}", method = RequestMethod.GET)
	public void getBatch(@PathVariable("queue") String queue, @RequestParam("quantity") int quantity) {
		JmsTemplate jms = ctx.getBean(JmsTemplate.class);
		for (int i = 0; i < quantity; i++) {
			jms.receiveAndConvert(queue);
		}
	}

	@RequestMapping(value = "/request/{queue}", method = RequestMethod.POST)
	public void sendRequest(@PathVariable("queue") String queue, @RequestParam("message") String message) {
		sendCustomMessage(queue, message);
	}

	@RequestMapping(value = "/batch/{queue}", method = RequestMethod.POST)
	public void sendBatch(@PathVariable("queue") String queue, @RequestParam("quantity") int quantity) {
		for (int i = 0; i < quantity; i++) {
			sendMessage(queue);
		}
	}

	@RequestMapping(value = "/raw", method = RequestMethod.POST)
	public void raw() {
		if (GlobalTracer.isRegistered()) {
			System.out.println("The global tracer is set");
			System.out.println(GlobalTracer.get().toString());
		}

		Span s = GlobalTracer.get().buildSpan("yeet").start();

		try (Scope sc = GlobalTracer.get().scopeManager().activate(s)) {
			s.setTag("tag", "please");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			s.finish();
		}
	}

	@RequestMapping(value = "/test", method = RequestMethod.POST)
	public void startTest(@RequestParam("queue") String queue, @RequestParam("quantity") int quantity) {

//		producer.setQueueName(queue);
//		--------------------------------------

//		Span span = tracer.buildSpan("testMessage").start();
//
//        HttpStatus status = HttpStatus.NO_CONTENT;
//
//        try {
//            int id = Integer.parseInt(idString);
//            log.info("Received Request to delete employee {}", id);
//            span.log(ImmutableMap.of("event", "delete-request", "value", idString));
//            if (employeeService.deleteEmployee(id, span)) {
//                span.log(ImmutableMap.of("event", "delete-success", "value", idString));
//                span.setTag("http.status_code", 200);
//                status = HttpStatus.OK;
//            } else {
//                span.log(ImmutableMap.of("event", "delete-fail", "value", "does not exist"));
//                span.setTag("http.status_code", 204);
//            }
//        } catch (NumberFormatException | NoSuchElementException nfe) {
//            span.log(ImmutableMap.of("event", "delete-fail", "value", idString));
//            span.setTag("http.status_code", 204);
//        }
//
//        span.finish();

//        -----------------------------

		consumer.setQueueName(queue);

		for (int i = 0; i < quantity; i++) {
			sendMessage(queue);
		}

		Thread consumerThread = new Thread(consumer);
		consumerThread.start();

//        Thread producerThread = new Thread(producer);
//        producerThread.start();
	}

	public void sendMessage(String queue) {
		sendCustomMessage(queue, java.util.UUID.randomUUID().toString());
	}

	public void sendCustomMessage(String queue, String message) {

		JmsTemplate jms = ctx.getBean(JmsTemplate.class);
		jms.convertAndSend(queue, message);
	}

	@RequestMapping(value = "/externalRequest", method = RequestMethod.POST)
	public void externalRequest() throws IOException {
		client = new OkHttpClient();
		Tracer t = GlobalTracer.get();
		
		HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(8081).addPathSegment("api/apiTrace")
                .addQueryParameter("param", "value").build();

		Span newSpan = t.buildSpan("external_span").start();
		
		t.scopeManager().activate(newSpan);

		newSpan.setTag("more_baggage", "super_bags");
		
		Request.Builder requestBuilder = new Request.Builder().url(url);

		Tags.SPAN_KIND.set(t.activeSpan(), Tags.SPAN_KIND_CLIENT);
		Tags.HTTP_METHOD.set(t.activeSpan(), "POST");
		Tags.HTTP_URL.set(t.activeSpan(), url.toString());		
		t.inject(t.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));

		Request r1 = requestBuilder.build();
		
		Response response = client.newCall(r1).execute();
		
		newSpan.finish();

		String json = "{\"id\":1,\"name\":\"John\"}";

		okhttp3.RequestBody body = okhttp3.RequestBody.create(MediaType.parse("application/json"), json);

		// "http://localhost:8081/api/apiTrace" -H "accept: */*" -H
		// "request: Content-Type: application/json" -H
		// "Content-Type: application/json" -d "{\"yes\" : \"no\"}"

		Request request = new Request.Builder().url("http://localhost:8081/api/apiTrace")
				.tag(new TagWrapper(newSpan.context())).post(body).build();

		Call call = client.newCall(request);
		call.execute();
	}
}
