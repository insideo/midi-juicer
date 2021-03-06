package org.randomcoder.midi.mac.coremidi;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.randomcoder.midi.mac.corefoundation.CFStringRef;
import org.randomcoder.midi.mac.corefoundation.CoreFoundationPeer;
import org.randomcoder.midi.mac.corefoundation.CoreFoundationServiceFactory;
import org.randomcoder.midi.mac.system.SystemPeer;
import org.randomcoder.midi.mac.system.SystemServiceFactory;

import javax.sound.midi.MidiMessage;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CoreMidi {

  private static CoreMidi INSTANCE = new CoreMidi();

  public static CoreMidi getInstance() {
    return INSTANCE;
  }

  static void setInstance(CoreMidi instance) {
    INSTANCE = instance;
  }

  private final Memory notifyRefCon;

  private final Map<Integer, MIDINotifyProc> notifyProcs =
      new ConcurrentHashMap<>();
  private final Map<Integer, MIDIReadProc> readProcs =
      new ConcurrentHashMap<>();

  CoreMidi() {
    notifyRefCon = new Memory(8L);
    notifyRefCon.setLong(0L, 0L);
  }

  CoreFoundationPeer cf() {
    return CoreFoundationServiceFactory.getPeer();
  }

  CoreMidiPeer peer() {
    return CoreMidiServiceFactory.getPeer();
  }

  SystemPeer sp() {
    return SystemServiceFactory.getPeer();
  }

  CoreMidiPropertyResolver resolver() {
    return CoreMidiServiceFactory.getPropertyResolver();
  }

  public void closeOutputPort(int outputPortId) {
    peer().MIDIPortDispose(outputPortId);
  }

  public void closeInputPort(int inputPortId) {
    peer().MIDIPortDispose(inputPortId);
    readProcs.remove(Integer.valueOf(inputPortId));
  }

  public void closeClient(int clientId) {
    peer().MIDIClientDispose(clientId);
    notifyProcs.remove(Integer.valueOf(clientId));
  }

  public void closeSource(int sourceId) {
    peer().MIDIEndpointDispose(sourceId);
  }

  public void closeDestination(int destId) {
    peer().MIDIEndpointDispose(destId);
    readProcs.remove(Integer.valueOf(destId));
  }

  public int getDeviceRefByUniqueID(int uniqueId) {
    IntByReference sourceRef = new IntByReference();
    IntByReference sourceTypeRef = new IntByReference();
    int result =
        peer().MIDIObjectFindByUniqueID(uniqueId, sourceRef, sourceTypeRef);

    if (result != 0) {
      throw CoreMidiException.fromError(result);
    }
    return sourceRef.getValue();
  }

  public int createSource(int clientId, String name) {
    CoreMidiPeer peer = peer();
    CoreFoundationPeer cf = cf();

    CFStringRef sourceName = CFStringRef.createNative(name);
    try {
      IntByReference outSrcRef = new IntByReference();

      int result = peer.MIDISourceCreate(clientId, sourceName, outSrcRef);
      if (result != 0) {
        throw CoreMidiException.fromError(result);
      }
      return outSrcRef.getValue();
    } finally {
      cf.CFRelease(sourceName.getPointer());
    }
  }

  public int createDestination(String name, int clientId,
      MIDIReadProc handler) {
    Objects.requireNonNull(handler);

    CoreMidiPeer peer = peer();
    CoreFoundationPeer cf = cf();

    CFStringRef destName = CFStringRef.createNative(name);
    try {
      IntByReference outDestRef = new IntByReference();

      int result = peer.MIDIDestinationCreate(clientId, destName, handler, null,
          outDestRef);
      if (result != 0) {
        throw CoreMidiException.fromError(result);
      }
      int outDestId = outDestRef.getValue();
      readProcs.put(Integer.valueOf(outDestId), handler);
      return outDestId;
    } finally {
      cf.CFRelease(destName.getPointer());
    }
  }

  public int createOutputPort(String name, int clientId) {
    CoreMidiPeer peer = peer();
    CoreFoundationPeer cf = cf();

    CFStringRef outputName = CFStringRef.createNative(name);
    try {
      IntByReference outputPortRef = new IntByReference();

      int result =
          peer.MIDIOutputPortCreate(clientId, outputName, outputPortRef);
      if (result != 0) {
        throw CoreMidiException.fromError(result);
      }
      return outputPortRef.getValue();
    } finally {
      cf.CFRelease(outputName.getPointer());
    }
  }

  public int createInputPort(String name, int clientId, MIDIReadProc handler) {
    Objects.requireNonNull(handler);

    CoreMidiPeer peer = peer();
    CoreFoundationPeer cf = cf();

    CFStringRef inputName = CFStringRef.createNative(name);
    try {
      IntByReference inputPortRef = new IntByReference();

      int result = peer.MIDIInputPortCreate(clientId, inputName, handler, null,
          inputPortRef);
      if (result != 0) {
        throw CoreMidiException.fromError(result);
      }
      int inputPortId = inputPortRef.getValue();
      readProcs.put(Integer.valueOf(inputPortId), handler);
      return inputPortId;
    } finally {
      cf.CFRelease(inputName.getPointer());
    }
  }

  public Pointer connectSource(int port, int source) {
    CoreMidiPeer peer = peer();

    Memory connRefCon = new Memory(8);
    connRefCon.setLong(0L, 0L);

    int result = peer.MIDIPortConnectSource(port, source, connRefCon);
    if (result != 0) {
      throw CoreMidiException.fromError(result);
    }

    return connRefCon;
  }

  public void disconnectSource(int port, int source, Pointer connRef) {
    CoreMidiPeer peer = peer();
    peer.MIDIPortDisconnectSource(port, source);
  }

  public int createClient(String name, MIDINotifyProc proc) {
    CoreMidiPeer peer = peer();
    CoreFoundationPeer cf = cf();

    CFStringRef nameRef = CFStringRef.createNative(name);
    try {
      IntByReference clientPtr = new IntByReference();

      int result =
          peer.MIDIClientCreate(nameRef, proc, notifyRefCon, clientPtr);
      if (result != 0) {
        throw CoreMidiException.fromError(result);
      }
      int clientId = clientPtr.getValue();
      notifyProcs.put(Integer.valueOf(clientId), proc);
      return clientId;
    } finally {
      cf.CFRelease(nameRef.getPointer());
    }
  }

  public void midiReceived(MidiMessage message, long timestamp, int sourceId) {
    CoreMidiPeer peer = peer();
    SystemPeer sp = sp();

    long ts = sp.mach_absolute_time();

    MIDIPacketList pl = new MIDIPacketList();
    pl.getPackets().add(new MIDIPacket(ts, message.getMessage()));
    Pointer p = pl.write();
    int result = peer.MIDIReceived(sourceId, p);

    if (result != 0) {
      throw CoreMidiException.fromError(result);
    }
  }

  public void sendMidi(MidiMessage message, long timestamp, int outputPortId,
      int destinationId) {

    CoreMidiPeer peer = peer();
    SystemPeer sp = sp();

    long ts = sp.mach_absolute_time();

    MIDIPacketList pl = new MIDIPacketList();
    pl.getPackets().add(new MIDIPacket(ts, message.getMessage()));
    Pointer p = pl.write();
    int result = peer.MIDISend(outputPortId, destinationId, p);

    if (result != 0) {
      throw CoreMidiException.fromError(result);
    }
  }

  public int getNumberOfDevices() {
    return peer().MIDIGetNumberOfDevices();
  }

  public int getNumberOfSources() {
    return peer().MIDIGetNumberOfSources();
  }

  public int getNumberOfDestinations() {
    return peer().MIDIGetNumberOfDestinations();
  }

  public int getSource(int sourceIndex0) {
    return peer().MIDIGetSource(sourceIndex0);
  }

  public int getDestination(int destIndex0) {
    return peer().MIDIGetDestination(destIndex0);
  }

  public String getStringProperty(CoreMidiProperty prop, int handle) {
    return stringProperty(cf(), peer(), resolver(), handle, prop);
  }

  public Integer getIntegerProperty(CoreMidiProperty prop, int handle) {
    return intProperty(peer(), resolver(), handle, prop);
  }

  protected Integer intProperty(CoreMidiPeer peer,
      CoreMidiPropertyResolver resolver, int device,
      CoreMidiProperty property) {

    CFStringRef propRef = property.resolve(resolver);

    Memory m = new Memory(4);
    int status = peer.MIDIObjectGetIntegerProperty(device, propRef, m);
    CoreMidiError error = CoreMidiError.byErrorCode(status);
    if (error == CoreMidiError.kMIDIUnknownProperty) {
      return null;
    } else if (error != CoreMidiError.kNoError) {
      throw CoreMidiException.fromError(error.errorCode());
    }
    return m.getInt(0);
  }

  protected String stringProperty(CoreFoundationPeer cf, CoreMidiPeer peer,
      CoreMidiPropertyResolver resolver, int device,
      CoreMidiProperty property) {
    CFStringRef propRef = property.resolve(resolver);

    Memory m = new Memory(8);
    int status = peer.MIDIObjectGetStringProperty(device, propRef, m);
    CoreMidiError error = CoreMidiError.byErrorCode(status);
    if (error == CoreMidiError.kMIDIUnknownProperty) {
      return null;
    } else if (error != CoreMidiError.kNoError) {
      throw CoreMidiException.fromError(error.errorCode());
    }

    Pointer ptr = m.getPointer(0);
    if (Pointer.nativeValue(ptr) == 0L) {
      return null;
    }
    CFStringRef r = new CFStringRef(ptr);
    String result = r.toString();
    cf.CFRelease(r.getPointer());
    return result;
  }

}
