# war-revision-maven-plugin
scan JSP/HTML files in webapp and append revision parameter to JS/CSS file links 

该插件用于在打包时自动改写webapp中的jsp文件, 在文件中所有css/js静态文件URL上追加一个版本号参数, 以实现在重新打包部署后, 让浏览器可以绕过缓存重新加载静态文件.<br/>
原理: 插件扫描工程内所有的HTML/JSP文件,找到其中的script/link标签, 将标签内的*.js, *.css 文件的链接后面追加一个_v参数, _v的值为执行该插件时的时间戳. <br/>

该插件需配合maven-war-plugin插件来使用, 必须配置在maven-war-plugin之前执行, 并将maven-war-plugin的warSourceDirectory参数设置为 ${project.build.directory}/${project.build.finalName}-rev.</br>
其原理是: war-revision-maven-plugin 执行后,将webapp目录下的文件全部拷贝到${project.build.directory}/${project.build.finalName}-rev 目录,扫描其中的文件,找到文件中的js/css引用链接并追加上_v参数.然后maven-war-plugin以该目录为源目录构建war包,这样war包的内容就是被修改过的.

#Usage:

  将插件部署到本地仓库,然后修改目标工程的POM.xml,如下:

    <plugins>
    	<plugin>
            <groupId>com.zxcl</groupId>
            <artifactId>war-revision-maven-plugin</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <configuration></configuration>
            <executions>
	          <execution>
	            <phase>compile</phase>
	            <goals>
	              <goal>revision</goal>
	            </goals>
	          </execution>
	        </executions>
      </plugin>
        
      <plugin>
          <artifactId>maven-war-plugin</artifactId>
          <configuration>
              <warSourceDirectory>${project.build.directory}/${project.build.finalName}-rev</warSourceDirectory>
          </configuration>
      </plugin>
    </plugins>
