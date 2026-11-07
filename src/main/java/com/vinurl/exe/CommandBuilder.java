package com.vinurl.exe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal replacement for org.apache.commons.exec.CommandLine used by the original mod.
 * Builds the argument list for an external process.
 */
public final class CommandBuilder {
	private final List<String> arguments = new ArrayList<>();

	public CommandBuilder(String executable) {
		this.arguments.add(executable);
	}

	public CommandBuilder addArguments(String[] args, boolean maskable) {
		this.arguments.addAll(Arrays.asList(args));
		return this;
	}

	/**
	 * Equivalent to commons-exec's addArguments(String): the argument string is
	 * tokenized on whitespace so e.g. "--cookies /path/to/file" becomes two arguments.
	 */
	public CommandBuilder addArguments(String arguments) {
		if (arguments != null && !arguments.isBlank()) {
			this.arguments.addAll(Arrays.asList(arguments.trim().split("\\s+")));
		}
		return this;
	}

	public String[] toStrings() {
		return this.arguments.toArray(new String[0]);
	}
}
