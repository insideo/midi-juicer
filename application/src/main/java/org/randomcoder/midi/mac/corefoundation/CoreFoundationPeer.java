package org.randomcoder.midi.mac.corefoundation;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface CoreFoundationPeer extends Library {

  public CFStringRef CFStringCreateWithCharacters(Pointer alloc, char[] chars,
      int numChars);

  public void CFStringGetCharacters(CFStringRef theString,
      CFRange.ByValue range, Pointer buffer);

  public Pointer CFStringGetCharactersPtr(CFStringRef theString);

  public int CFStringGetLength(CFStringRef theString);

  public Pointer CFRetain(Pointer cf);

  public void CFRelease(Pointer cf);

  public int CFGetRetainCount(Pointer cf);

  public CFRange.ByValue __CFRangeMake(long loc, long len);

  public void CFRunLoopRun();

  public int CFRunLoopRunInMode(CFStringRef mode, double seconds,
      boolean returnAfterSourceHandled);

  public Pointer CFRunLoopGetCurrent();

  public void CFRunLoopStop(Pointer rl);

  public Pointer CFRunLoopSourceCreate(Pointer alloc, int order,
      CFRunLoopSourceContext context);

  public void CFRunLoopAddSource(Pointer rl, Pointer source, CFStringRef mode);

  public void CFRunLoopRemoveSource(Pointer rl, Pointer source,
      CFStringRef mode);

  public void CFRunLoopSourceSignal(Pointer source);

}
