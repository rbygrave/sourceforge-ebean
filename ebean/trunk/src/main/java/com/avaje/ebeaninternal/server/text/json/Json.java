package com.avaje.ebeaninternal.server.text.json;

import com.avaje.ebean.text.json.JsonElement;

public class Json {

	public static JsonElement parse(String s) {
		
		ReadJsonSourceString src = new ReadJsonSourceString(s);
		ReadBasicJsonContext b = new ReadBasicJsonContext(src);
		return ReadJsonRawReader.readJsonElement(b);
	}
}
