package com.example.openTracing;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {

	@RequestMapping("/")
	public String index() {
		return "<a href=\"http://localhost:8081/swagger-ui.html#/tracing-resource\">API test page</a>";
	}

}