package com.example.openTracing.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.openTracing.util.RequestBuilderCarrier;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

import java.io.IOException;

/**
 * 
 * This class sends api calls with OpenTracing tags to the receiver on the same system
 * 
 * Notes: 
 * JmsTemplate is used for completely synchronous jms client calls
 * 
 * Tracer logic is intentionally grouped as much as possible here to clearly show
 * the necessary steps when initializing and sending traces. In a more mature codebase
 * it would make more sense to split out certain steps.
 */
@Service
@Component
@RestController
@RequestMapping("/api")
public class TracingResource {

	@Autowired
	private ApplicationContext ctx;

	private OkHttpClient client;
	
	public TracingResource() {
		client = new OkHttpClient();
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

		t.scopeManager().activate(newSpan);

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
}
