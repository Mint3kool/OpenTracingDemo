package com.example.openTracing.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.opentracing.propagation.TextMap;

public class HttpHeadersExtract implements TextMap {
	private final Map<String, String> map = new HashMap<>();

	public HttpHeadersExtract(final Map<String, String> headers) {
		for (Map.Entry<String, String> entry : headers.entrySet())
			map.put(entry.getKey(), entry.getValue());
	}

	@Override
	public void put(String arg0, String arg1) {
		throw new UnsupportedOperationException("This class can be used only with Tracer.inject()");
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		return map.entrySet().iterator();
	}

}
