package org.randomcoder.midi.corefoundation;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class CoreFoundationServiceFactory {
	private static final AtomicReference<CoreFoundationPeer> PEER = new AtomicReference<>();
	private static final AtomicReference<NativeLibrary> NATIVE_LIBRARY = new AtomicReference<>();

	public static final String LIBRARY_NAME = "CoreFoundation";

	public static CoreFoundationPeer getPeer() {
		return getOrCreate(PEER, CoreFoundationServiceFactory::createNativePeer);
	}

	public static NativeLibrary getNativeLibrary() {
		return getOrCreate(NATIVE_LIBRARY, CoreFoundationServiceFactory::createNativeLibrary);
	}

	static CoreFoundationPeer createNativePeer() {
		return (CoreFoundationPeer) Native.loadLibrary(LIBRARY_NAME, CoreFoundationPeer.class);
	}

	static NativeLibrary createNativeLibrary() {
		return NativeLibrary.getInstance(LIBRARY_NAME);
	}

	static void setPeer(CoreFoundationPeer peer) {
		forceSet(PEER, peer);
	}

	static void setNativeLibrary(NativeLibrary lib) {
		forceSet(NATIVE_LIBRARY, lib);
	}

	private static <T> void forceSet(AtomicReference<T> ref, T obj) {
		synchronized (ref) {
			ref.set(obj);
		}
	}

	private static <T> T getOrCreate(AtomicReference<T> ref, Supplier<T> supplier) {
		T obj;
		while ((obj = ref.get()) == null) {
			// if object is null, we synchronize, and test again
			synchronized (ref) {
				obj = ref.get();
				if (obj != null) {
					// some other thread set it already
					break;
				}
				// create and update the reference
				ref.set(supplier.get());
				obj = ref.get();
			}
		}
		return obj;
	}
}