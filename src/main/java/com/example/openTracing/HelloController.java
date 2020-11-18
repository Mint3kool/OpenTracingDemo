package com.example.openTracing;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Landing page of localhost:8080
 * 
 * @author tshen
 *
 */
@RestController
public class HelloController {

	/**
	 * html url that points to the Swagger API resources
	 */
	@RequestMapping("/")
	public String index() {
		return "<a href=\"http://localhost:8080/swagger-ui.html#/tracing-resource\">API test page</a>";
	}

}