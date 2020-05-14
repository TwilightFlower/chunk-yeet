package io.github.nuclearfarts.chunkyeet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

public class EarlyBoundHelper {
	private static final MappingResolver MR = FabricLoader.getInstance().getMappingResolver();
	
	public static MethodHandle getEarlyBound(Class<?> owner, String intName, MethodType descriptor, Class<?> invokeFrom) {
		return getEarlyBound(MethodHandles.publicLookup(), owner, intName, descriptor, invokeFrom);
	}
	
	public static MethodHandle getEarlyBound(MethodHandles.Lookup lookup, Class<?> owner, String intName, MethodType descriptor, Class<?> invokeFrom) {
		String unmappedOwner = MR.unmapClassName("intermediary", owner.getName());
		String unmappedDesc = unmapDesc(descriptor);
		String mappedName = MR.mapMethodName("intermediary", unmappedOwner, intName, unmappedDesc);
		try {
			return lookup.findSpecial(owner, mappedName, descriptor, invokeFrom);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static String unmapDesc(MethodType descriptor) {
		StringBuilder sb = new StringBuilder("(");
		for(Class<?> clazz : descriptor.parameterArray()) {
			sb.append(unmap(clazz));
		}
		sb.append(')');
		sb.append(unmap(descriptor.returnType()));
		return sb.toString();
	}
	
	private static String unmap(Class<?> clazz) {
		if(clazz.isPrimitive()) {
			switch(clazz.getName()) {
			case "boolean":
				return "Z";
			case "int":
				return "I";
			case "short":
				return "S";
			case "byte":
				return "B";
			case "long":
				return "J";
			case "double":
				return "D";
			case "float":
				return "F";
			default:
				throw new RuntimeException("Unknown primitive type " + clazz);
			}
		} else {
			return "L" + MR.unmapClassName("intermediary", clazz.getName()) + ";";
		}
	}
}
