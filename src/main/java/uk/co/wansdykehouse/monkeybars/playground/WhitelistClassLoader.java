package uk.co.wansdykehouse.monkeybars.playground;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class WhitelistClassLoader extends ClassLoader {

	private static final Set<String> whitelist = new HashSet<>();
	
	private ClassLoader parent;

	static {
		try(BufferedReader br = new BufferedReader(new InputStreamReader(Thread.currentThread().getClass().getResourceAsStream("/whitelist.txt")))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().length() > 0) {
					whitelist.add(line.trim());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public WhitelistClassLoader(ClassLoader parent) {
		super(parent);
		this.parent = parent;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (!(name.startsWith("java.") || name.startsWith("javax.")) || whitelist.contains(name)) {
			if (!name.startsWith("java.") && !name.startsWith("javax.")) {
				return getClass(name);
			} else {
				return super.loadClass(name);
			}
		} else {
			throw new RuntimeException("This class [" + name + "] is not whitelisted.");
		}

	}

	private Class<?> getClass(String name) throws ClassNotFoundException {
		String file = name.replace('.', File.separatorChar) + ".class";
		
		try {
			byte[] b = loadClassData(file);
			Class<?> c = defineClass(name, b, 0, b.length);
			resolveClass(c);
			return c;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private byte[] loadClassData(String name) throws IOException {
		InputStream stream = parent.getResourceAsStream(name);
		int size = stream.available();
		byte buff[] = new byte[size];
		DataInputStream in = new DataInputStream(stream);
		in.readFully(buff);
		in.close();
		return buff;
	}
}