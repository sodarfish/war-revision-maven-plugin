package com.zxcl.war_revision_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 该插件扫描工程内所有的HTML/JSP文件,找到其中的script/link标签, 将标签内的*.js, *.css 文件的链接后面追加一个_v参数, 该参数的值为执行该插件时的时间戳. <br/>
 * 该插件需配合maven-war-plugin插件来使用, 必须再maven-war-plugin之前执行, 并将maven-war-plugin 的 warSourceDirectory 设置为 ${project.build.directory}/${project.build.finalName}-rev,</br>
 * 其原理是: war-revision-maven-plugin 执行后,将替换过的webapp目录下的文件写入 ${project.build.directory}/${project.build.finalName}-rev 目录,<br/> 
 * 然后maven-war-plugin以该目录为源目录构建war包,
 * 这样war包内的内容就是被修改过的.
 * 
 * <pre>&lt;plugins>
    	&lt;plugin>
            &lt;groupId>com.zxcl&lt;/groupId>
            &lt;artifactId>war-revision-maven-plugin&lt;/artifactId>
            &lt;version>0.0.1-SNAPSHOT&lt;/version>
            &lt;configuration>&lt;/configuration>
            &lt;executions>
	          &lt;execution>
	            &lt;phase>package&lt;/phase>
	            &lt;goals>
	              &lt;goal>revision&lt;/goal>
	            &lt;/goals>
	          &lt;/execution>
	        &lt;/executions>
        &lt;/plugin>
        
        &lt;plugin>
            &lt;artifactId>maven-war-plugin&lt;/artifactId>
            &lt;configuration>
               &lt;warSourceDirectory>${project.build.directory}/${project.build.finalName}-rev&lt;/warSourceDirectory>
            &lt;/configuration>
         &lt;/plugin>
    &lt;/plugins>
 </pre>
 * @author Chang Wen
 */
@Mojo(name="revision")
public class WarRevisionMojo extends AbstractMojo {

	@Parameter(defaultValue = "${basedir}/src/main/webapp", required = true)
	private File warSourceDirectory;
	
	@Parameter(defaultValue = "${project.build.sourceEncoding}", required = false)
	private String encoding;
	
	@Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}-rev", required = true)
	private File revSourceDirectory;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		//append revision parameter to js/css/image url of all html & jsp files
		if(!warSourceDirectory.exists()) {
			throw new MojoFailureException("warSourceDirectory=" + warSourceDirectory.getAbsolutePath() + " does not exists!");
		}
		if(!warSourceDirectory.isDirectory()){
			throw new MojoFailureException("warSourceDirectory=" + warSourceDirectory.getAbsolutePath() + " is not a directory!");
		}
		
		if(encoding == null || encoding.length() == 0){
			encoding = Charset.defaultCharset().name();;
			getLog().warn("Using platform encoding (" + encoding + " actually) because you do not specify the (project.build.sourceEncoding) property in POM");
		}
		
		//copy sources to dest folder
		try {
			if(revSourceDirectory.exists())
				IOUtil.deleteDirectory(revSourceDirectory);
			IOUtil.copyDirectoryStructure(warSourceDirectory, revSourceDirectory);
		} catch (IOException e1) {
			throw new MojoExecutionException(e1.getMessage(),e1);
		}
		
		getLog().info("scan files to process, directory = " + revSourceDirectory);
		List<File> htmlFiles = getFilesToProcess(revSourceDirectory);
		String revisionNumber = System.currentTimeMillis() + "";
		
		Pattern p0 = Pattern.compile("<script\\s+[^>]+\"([^\"]+\\.js)\"[^>]*>");
		Pattern p1 = Pattern.compile("<script\\s+[^>]+'([^\"]+\\.js)\'[^>]*>");
		Pattern p2 = Pattern.compile("<link\\s+[^>]+\"([^\"]+\\.css)\"[^>]*>");
		Pattern p3 = Pattern.compile("<link\\s+[^>]+'([^\"]+\\.css)\'[^>]*>");
		
		for(File htmlFile : htmlFiles) {
			try {
				getLog().info("process file : " + htmlFile.getAbsolutePath());
				String html = IOUtil.readText(htmlFile, encoding);

				html = scanAndAppendRevision(p0,html, revisionNumber );

				html = scanAndAppendRevision(p1,html, revisionNumber );

				html = scanAndAppendRevision(p2,html, revisionNumber );

				html = scanAndAppendRevision(p3,html, revisionNumber );
				
				IOUtil.writeText(htmlFile, html, encoding);
				
			} catch (IOException e) {
				getLog().error(e.getMessage(), e);
			}
		}
	}
	
	protected List<File> getFilesToProcess(File sourceDirectory) throws MojoFailureException{
		
		List<File> result = new ArrayList<File>(); 
		
		File[] files = sourceDirectory.listFiles();
		for(File file : files) {
			if(file.isDirectory()) {
				result.addAll(getFilesToProcess(file));
			} else {
				if(file.getName().endsWith(".jsp") 
						|| file.getName().endsWith(".html")
						|| file.getName().endsWith(".htm")) {
					result.add(file);
				}
			}
		}
		return result;
		
	}
	
	private static String scanAndAppendRevision(Pattern p, String html, String revisionNumber) {
		Matcher m = p.matcher(html);
		StringBuffer sb = new StringBuffer();
		int start = 0;
		while(m.find()){
			String line = m.group(0);
			String name = m.group(1);
			String newLine = line.replace(name, name + "?_v=" + revisionNumber);
			
			sb.append(html.substring(start, m.start()));
			sb.append(newLine);
			start = m.end();
		}
		sb.append(html.substring(start));
		return sb.toString();
	}
}
