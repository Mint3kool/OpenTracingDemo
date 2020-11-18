package com.example.openTracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Exception {
		if (!configureGlobalTracer())
			throw new Exception("Could not configure the global tracer");

		SpringApplication.run(Application.class, args);
	}

	static boolean configureGlobalTracer() {
		Tracer t = null;
		Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
				.withType("const").withParam(1);
		Configuration.SenderConfiguration senderConfig = new Configuration.SenderConfiguration()
				.withAgentHost("localhost").withAgentPort(5775);
		Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
				.withLogSpans(true).withSender(senderConfig);
		t = new Configuration("global_tracer_sender").withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
		GlobalTracer.registerIfAbsent(t);
		return true;
	}
}