package com.telerik.metadata;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ActivityGenerator {
	
	private static class MethodComparer implements Comparator<Method>
	{
		@Override
		public int compare(Method arg0, Method arg1) {
			return arg0.getName().compareTo(arg1.getName());
		}
	}
	
	public void generate() throws Exception
	{
		NSClassLoader loader = NSClassLoader.getInstance();
		loader.loadDir("../jars");
		
		String baseActivity = "android.app.Activity";

		Class<?> clazz = Class.forName(baseActivity, false, loader);
		
		PrintWriter w = new PrintWriter("NativeScriptActivity.java");
		
		w.write("package com.tns;\n\n");
		
		w.write("public class NativeScriptActivity extends " + baseActivity + "\n");
		w.write("{\n");
		
		Constructor<?>[] ctors = clazz.getConstructors();
		for (Constructor<?> c: ctors)
		{
			int modifiers = c.getModifiers();
			
			if (!c.isSynthetic() && !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
				&& (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)))
			{
				if (Modifier.isPublic(modifiers))
				{
					w.write("\tpublic NativeScriptActivity");
				}
				else
				{
					w.write("\tprotected NativeScriptActivity");
				}
				writeSignature(w, c.getParameterTypes());
				writeThrowsClause(w, c.getExceptionTypes());
				w.write("\n");
				w.write("\t{\n");
				w.write("\t}\n");
				w.write("\n");
			}
		}
		
		Map<String, Integer> methodIdx = new HashMap<String, Integer>();
		
		ArrayList<Method> methods = new ArrayList<Method>();
		Method[] publicMethods = clazz.getMethods();
		for (Method m: publicMethods)
		{
			methods.add(m);
		}
		
		Method[] declMethods = clazz.getDeclaredMethods();
		for (Method m: declMethods)
		{
			int modifiers = m.getModifiers();
			
			if (!m.isSynthetic() && !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && Modifier.isProtected(modifiers))
			{
				methods.add(m);
			}
		}
		
		
		methods.sort(new MethodComparer());
		
		String lastMethodName = null;
		int idx = 0;
		for (Method m: methods)
		{
			int modifiers = m.getModifiers();
			
			if (!m.isSynthetic() && !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
				&& (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)))
			{
				if (!m.getName().equals(lastMethodName))
				{
					idx = methodIdx.size();
					lastMethodName = m.getName();
					methodIdx.put(m.getName(), idx);
				}
				
				if (Modifier.isPublic(modifiers))
				{
					w.write("\tpublic ");
				}
				else
				{
					w.write("\tprotected ");
				}
				Class<?> retTtype = m.getReturnType(); 
				w.write(retTtype.getCanonicalName() + " " + m.getName());
				writeSignature(w, m.getParameterTypes());
				writeThrowsClause(w, m.getExceptionTypes());
				w.write("\n");
				w.write("\t{\n");
				//
				w.write("\t\tif (__ho" + idx + ")\n");
				w.write("\t\t{\n");
				int pc = m.getParameterCount();
				w.write("\t\t\tObject[] args = " + ((pc == 0) ? "null;\n" : ("new Object[" + pc + "];\n")));
				for (int i=0; i<pc; i++)
				{
					w.write("\t\t\targs[" + i + "] = param_" + i + ";\n");
				}
				w.write("\t\t\t");
				if (!retTtype.equals(void.class))
				{
					w.write("return (" + retTtype.getCanonicalName() + ")");
				}
				w.write("com.tns.Platform.callJSMethod(this, \"" + m.getName() + "\", args);\n");

				w.write("\t\t}\n");
				w.write("\t\telse\n");
				w.write("\t\t{\n");
				w.write("\t\t\t");
				if (!retTtype.equals(void.class))
				{
					w.write("return ");
				}
				w.write("super." + m.getName());
				w.write("(");
				int count = 0;
				for (Class<?> p: m.getParameterTypes())
				{
					if (count > 0)
					{
						w.write(", ");
					}
					w.write("param_" + count++);
				}
				w.write(");\n");
				w.write("\t\t}\n");
				//
				w.write("\t}\n");
				w.write("\n");
			}
		}
		
		w.write("\tprivate void setMethodOverrides(String[] methodOverrides)\n");
		w.write("\t{\n");
		w.write("\t\tfor (String m: methodOverrides)\n");
		w.write("\t\t{\n");
		for (String m: methodIdx.keySet())
		{
			w.write("\t\t\tif (m.equals(\"" + m + "\")) __ho" + (int)methodIdx.get(m) + "= true;\n");	
		}
		
		w.write("\t\t}\n");
		w.write("\t}\n");
		
		for (int i=0; i<methodIdx.size(); i++)
		{
			w.write("\tprivate boolean __ho" + i + ";\n");
		}
		
		w.write("}\n");
		
		w.close();
	}
	
	private void writeSignature(PrintWriter w, Class<?>[] paramters)
	{
		w.write("(");
		int count = 0;
		for (Class<?> p: paramters)
		{
			if (count > 0)
			{
				w.write(", ");
			}
			w.write(p.getCanonicalName() + " param_" + count++);
			}
		w.write(")");
	}
	
	private void writeThrowsClause(PrintWriter w, Class<?>[] exceptions)
	{
		int count = 0;
		for (Class<?> e: exceptions)
		{
			if (count == 0)
			{
				w.write(" throws ");
			}
			else
			{
				w.write(", ");
			}
			w.write(e.getCanonicalName());
		}
	}
}
