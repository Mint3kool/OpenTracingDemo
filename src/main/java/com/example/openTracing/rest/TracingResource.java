package com.example.openTracing.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.openTracing.Consumer;
import com.example.openTracing.Producer;
import com.example.openTracing.util.HttpHeadersExtract;
import com.example.openTracing.util.RequestBuilderCarrier;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import java.io.IOException;

import java.util.Map;

import javax.jms.Message;

/**
 * Notes: 
 * 
 * JmsTemplate is used for completely synchronous jms client calls
 * 
 * I've also intentionally grouped all the tracer logic as much as possible here to clearly show
 * the necessary steps when initializing and sending traces
 */
@Service
@Component
@RestController
@RequestMapping("/api")
public class TracingResource {

	@Autowired
	private ApplicationContext ctx;

	private OkHttpClient client;

	private static final Logger logger = LoggerFactory.getLogger(TracingResource.class);
	
	public TracingResource() {
		client = new OkHttpClient();
	}

	@RequestMapping(value = "/trace", method = RequestMethod.GET)
	public void getTrace(@RequestParam("queue") String queue) {
		JmsTemplate jms = ctx.getBean(JmsTemplate.class);
		Tracer t = (Tracer) jms.receiveAndConvert(queue);

		Span s = t.buildSpan("jms_recieve").start();

		s.setTag("second", "2");
		s.finish();
	}

	public void sendMessage(String queue) {
		sendCustomMessage(queue, java.util.UUID.randomUUID().toString());
	}

	public void sendCustomMessage(String queue, String message) {

//		AmqpTemplate amqp = ctx.getBean(AmqpTemplate.class);
		JmsTemplate jms = ctx.getBean(JmsTemplate.class);
		jms.convertAndSend(queue, message);
	}

	@RequestMapping(value = "/nestedExternalRequest", method = RequestMethod.POST)
	public void nestedExternalRequest() throws IOException {
		Tracer t = GlobalTracer.get();

		HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(8081).addPathSegment("api")
				.addPathSegment("nestedApiTrace").addQueryParameter("param", "value").build();

		Span newSpan = t.buildSpan("top_external_span").start();

		t.scopeManager().activate(newSpan);

		newSpan.setTag("more_baggage", "super_bags");

		Request.Builder requestBuilder = new Request.Builder().url(url);

		Tags.SPAN_KIND.set(t.activeSpan(), Tags.SPAN_KIND_SERVER);
		Tags.HTTP_METHOD.set(t.activeSpan(), "GET");
		Tags.HTTP_URL.set(t.activeSpan(), url.toString());
		t.inject(t.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));

		Request r1 = requestBuilder.build();

		Response response = client.newCall(r1).execute();

		Tags.HTTP_STATUS.set(t.activeSpan(), response.code());

		newSpan.finish();
	}
	
	@RequestMapping(value = "/finishedExternalRequest", method = RequestMethod.POST)
	public void finishedExternalRequest() throws IOException {
		Tracer t = GlobalTracer.get();

		HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(8081).addPathSegment("api")
				.addPathSegment("finishedApiTrace").addQueryParameter("param", "value").build();

		Span newSpan = t.buildSpan("finished_external_span").start();

		Scope s = t.scopeManager().activate(newSpan);//, true);

		newSpan.setTag("more_baggage", "super_bags");
		
		Request.Builder requestBuilder = new Request.Builder().url(url);

		Tags.SPAN_KIND.set(t.activeSpan(), Tags.SPAN_KIND_SERVER);
		Tags.HTTP_METHOD.set(t.activeSpan(), "GET");
		Tags.HTTP_URL.set(t.activeSpan(), url.toString());
		t.inject(t.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));

		Request r1 = requestBuilder.build();
		
		newSpan.finish();

		Response response = client.newCall(r1).execute();

		Tags.HTTP_STATUS.set(t.activeSpan(), response.code());
	}
	

	public void sendCustomObjectMessage(String queue, Object o) {
		JmsTemplate jms = ctx.getBean(JmsTemplate.class);
		jms.convertAndSend(queue, o);
	}
}
