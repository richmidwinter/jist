package uk.co.wansdykehouse.monkeybars.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.co.wansdykehouse.monkeybars.playground.WhitelistClassLoader;

@Path("/")
public class Api {
	
	@GET
	@Path("/read/{id}")
	public Response read(@PathParam("id") String id) throws Exception {
		java.nio.file.Path path = Paths.get("/tmp/monkeybars", id, "Main.java");
		
		if (!Files.exists(path)) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		String source = Files.readAllLines(path, Charset.defaultCharset())
				 .stream()
				 .collect(Collectors.joining("\n"));
		 
		return Response.ok(source).build();
	}

	// FIXME: Limit request size.
	@POST
	@Path("/execute/{id}")
	public Response execute(@PathParam("id") String id, String source) throws Exception {
		// Persist
		String target = persist(id, source);
		
		// Alter stdout
		PrintStream stdout = System.out;
		PrintStream stderr = System.err;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream capture = new PrintStream(baos);
		
		try {
			System.setOut(capture);
			System.setErr(capture);
	
			// Compile source file.
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			int result = compiler.run(null, capture, capture, Paths.get(target.toString(), "/Main.java").toString());
			
			if (result == 0) { // Successful compile, execute.
				// Load and instantiate compiled class.
				WhitelistClassLoader classLoader = new WhitelistClassLoader(
						URLClassLoader.newInstance(new URL[] { new URL(Paths.get(target).toUri() +"/") }));
		
				// Run
				// FIXME: Execute in separate JVM.
				// FIXME: Kill off if running for too long.
		        String progClass = "Main";
		        String[] args = {};
		        Class<?> clazz = classLoader.loadClass(progClass);
		        Class<?> mainArgType[] = { (new String[0]).getClass() };
		        Method main = clazz.getMethod("main", mainArgType);
		        Object argsArray[] = { args };
		        main.invoke(null, argsArray);
			}
			
	        // Return output of last successful command
			return Response.ok(baos.toString()).build();
		} finally {
	        // Replace stdout
	        System.out.flush();
	        System.err.flush();
	        
	        System.setOut(stdout);
	        System.setErr(stderr);
		}
	}
	
	private String persist(String id, String source) throws Exception {
		String target = Paths.get("/tmp/monkeybars", id).toString();
		Files.createDirectories(Paths.get(target));
		
		File sourceFile = new File(target +"/Main.java");
		Files.write(sourceFile.toPath(), source.getBytes());
		
		return target;
	}
}
