package net.aegistudio.transparent.opengl.glsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import net.aegistudio.transparent.opengl.util.FeatureNotSupportedException;
import net.aegistudio.transparent.util.BindingFailureException;
import net.aegistudio.transparent.util.Resource;

import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public class Shader implements Resource
{
	private String glslSource;
	private EnumShaderType glslShaderType;
	private int glslShaderId;
	
	public Shader(String glslSource, EnumShaderType shaderType)
	{
		this.glslSource = glslSource;
		this.glslShaderType = shaderType;
		this.glslShaderId = 0;
	}
	
	public void create()
	{
		if(this.glslShaderId == 0)
		{
			if(!GLContext.getCapabilities().GL_ARB_shader_objects)
				throw new FeatureNotSupportedException("shading languague object");
			
			if(!this.glslShaderType.checkCapability())
				throw new FeatureNotSupportedException(this.glslShaderType.shaderName);
			
			this.glslShaderId = ARBShaderObjects.glCreateShaderObjectARB(glslShaderType.stateId);
			if(this.glslShaderId == 0) throw new BindingFailureException("Unable to allocate space for shader object!");
			
			ARBShaderObjects.glShaderSourceARB(glslShaderId, glslSource);
			ARBShaderObjects.glCompileShaderARB(glslShaderId);
			
			if(ARBShaderObjects.glGetObjectParameteriARB(glslShaderId, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
			{
				String failureInfo = checkStatus(glslShaderId);
				CompileFailureException compileFailureException = new CompileFailureException("Unable to compile shader objects, cause by: " + failureInfo);
				ARBShaderObjects.glDeleteObjectARB(this.glslShaderId);
				this.glslShaderId = 0;
				throw compileFailureException;
			}
		}
	}
	
	public void destroy()
	{
		if(this.glslShaderId != 0)
		{
			ARBShaderObjects.glDeleteObjectARB(this.glslShaderId);
			this.glslShaderId = 0;
		}
	}
	
	public int getShaderObjectId()
	{
		return this.glslShaderId;
	}
	
	static String checkStatus(int glslObjectId)
	{
		return ARBShaderObjects.glGetInfoLogARB(glslObjectId, 
				ARBShaderObjects.glGetObjectParameteriARB(glslObjectId, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
	}
	
	public static Shader createShaderFromSourceFile(File sourceFile, EnumShaderType shaderType) throws Exception
	{
		if(!sourceFile.exists()) throw new FileNotFoundException();
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader buffReader = new BufferedReader(new FileReader(sourceFile));
		String currentLine = null;
		while((currentLine = buffReader.readLine()) != null) stringBuilder.append(currentLine).append('\n');
		buffReader.close();
		return new Shader(new String(stringBuilder), shaderType);
	}
	
	public void finalize() throws Throwable
	{
		this.destroy();
		super.finalize();
	}
}