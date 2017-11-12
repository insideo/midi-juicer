package org.randomcoder.midi.coremidi;

import java.util.ArrayList;
import java.util.List;

import org.randomcoder.midi.corefoundation.CFStringRef;
import org.randomcoder.midi.corefoundation.CoreFoundationPeer;
import org.randomcoder.midi.corefoundation.CoreFoundationServiceFactory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class CoreMidi {
	private static CoreMidi INSTANCE = new CoreMidi();

	public static CoreMidi getInstance() {
		return INSTANCE;
	}

	static void setInstance(CoreMidi instance) {
		INSTANCE = instance;
	}

	CoreMidi() {

	}

	CoreFoundationPeer cf() {
		return CoreFoundationServiceFactory.getPeer();
	}

	CoreMidiPeer peer() {
		return CoreMidiServiceFactory.getPeer();
	}

	CoreMidiPropertyResolver resolver() {
		return CoreMidiServiceFactory.getPropertyResolver();
	}

	public void createClient() {
		CoreMidiPeer peer = peer();

		CFStringRef name = CFStringRef.createNative("Darwin Native CoreMIDI");
		Memory notifyRefCon = new Memory(8);
		notifyRefCon.setLong(0L, 12345L);
		IntByReference clientPtr = new IntByReference();

		MIDINotifyProc proc = (message, refCon) -> {
			System.out.printf("MIDINotifyProc message: %s refCon: %s%n", message, refCon);
		};

		// create a client
		int result = peer.MIDIClientCreate(name, proc, notifyRefCon, clientPtr);
		System.out.printf("MIDIClientCreate Result: %s%n", result);

		int client = clientPtr.getValue();
		System.out.printf("Client: %d%n", client);

		CFStringRef portName = CFStringRef.createNative("Darwin Native CoreMIDI - Port 1");

		MIDIReadProc readProc = (pktlist, readProcRefCon, srcConnRefCon) -> {
			// convert readProcRefCon into something useful
			long readProcRefConValue = readProcRefCon.getLong(0L);

			System.out.printf("MIDIReadProc pktlist: %s readProcRefCon: %d srcConnRefCon: %s%n",
					pktlist, readProcRefConValue, srcConnRefCon);

			// go through packets
			MIDIPacketList pList = new MIDIPacketList(pktlist, 0);
			System.out.printf("Packet list: %s%n", pList);

			for (int i = 0; i < pList.numPackets; i++) {
				MIDIPacket packet = pList.packet[i];
				System.out.printf("Packet: %s%n", packet);
				System.out.print("  data (hex):");
				for (short j = 0; j < packet.length; j++) {
					System.out.printf("  %02x", packet.data[j]);
				}
				System.out.println();
				System.out.print("  data (dec):");
				for (int j = 0; j < packet.length; j++) {
					System.out.printf(" %3d", packet.data[j] & 0xff);
				}
				System.out.println();
			}
		};

		// create an input port
		Memory inputPortRefCon = new Memory(8);
		inputPortRefCon.setLong(0L, 34567L);
		CFStringRef inputName = CFStringRef.createNative("Input");
		IntByReference inputPortRef = new IntByReference();
		result = peer.MIDIInputPortCreate(client, inputName, readProc, inputPortRefCon, inputPortRef);
		System.out.printf("MIDIInputPortCreate Result: %s%n", result);

		int inputPort = inputPortRef.getValue();
		System.out.printf("Input Port: %d%n", inputPort);

		// find the source corresponding to MidiPipe's virtual output 1
		// NOTE: ID is not consistent between runs :(
		IntByReference sourceRef = new IntByReference();
		IntByReference sourceTypeRef = new IntByReference();
		result = peer.MIDIObjectFindByUniqueID(-1301456318, sourceRef, sourceTypeRef);
		System.out.printf("MIDIObjectFindByUniqueID Result: %s%n", result);

		int source = sourceRef.getValue();
		System.out.printf("MidiPipe Source: %d%n", source);
		
		// connect input port to source
		Memory connRefCon = new Memory(8);
		inputPortRefCon.setLong(0L, 45678);
		result = peer.MIDIPortConnectSource(inputPort, source, connRefCon);
		System.out.printf("MIDIPortConnectSource Result: %s%n", result);
		
		// create a destination
		Memory readRefCon = new Memory(8);
		readRefCon.setLong(0L, 23456L);
		IntByReference destPtr = new IntByReference();

		result = peer.MIDIDestinationCreate(client, portName, readProc, readRefCon, destPtr);
		System.out.printf("MIDIDestinationCreate Result: %s%n", result);

		int dest = destPtr.getValue();
		System.out.printf("Destination: %d%n", dest);

		try {
			Thread.sleep(600_000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		result = peer.MIDIClientDispose(client);
		System.out.printf("MIDIClientDispose Result: %s%n", result);
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

	public void getDevices() {
		CoreFoundationPeer cf = cf();
		CoreMidiPeer peer = peer();
		CoreMidiPropertyResolver resolver = resolver();

		List<Integer> online = new ArrayList<>();
		int count = peer.MIDIGetNumberOfDevices();
		for (int i = 0; i < count; i++) {
			// online.add(peer.MIDIGetDevice(i));
		}

		int sourceCount = peer.MIDIGetNumberOfSources();
		for (int i = 0; i < sourceCount; i++) {
			online.add(peer.MIDIGetSource(i));
		}

		int destCount = peer.MIDIGetNumberOfDestinations();
		for (int i = 0; i < destCount; i++) {
			online.add(peer.MIDIGetDestination(i));
		}

		for (int dev : online) {
			String name = stringProperty(cf, peer, resolver, dev, CoreMidiProperty.kMIDIPropertyName);
			String manufacturer = stringProperty(cf, peer, resolver, dev, CoreMidiProperty.kMIDIPropertyManufacturer);
			String model = stringProperty(cf, peer, resolver, dev, CoreMidiProperty.kMIDIPropertyModel);
			Integer uniqueId = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyUniqueID);
			Integer deviceId = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyDeviceID);

			// these are endpoint only
			// Integer receiveChannels = intProperty(peer, resolver, dev,
			// CoreMidiProperty.kMIDIPropertyReceiveChannels);
			// Integer transmitChannels = intProperty(peer, resolver, dev,
			// CoreMidiProperty.kMIDIPropertyTransmitChannels);

			Integer maxSysExSpeed = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyMaxSysExSpeed);
			Integer advanceScheduleTimeMuSec = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyAdvanceScheduleTimeMuSec);

			// endpoint only
			// Integer isEmbeddedEntity = intProperty(peer, resolver, dev,
			// CoreMidiProperty.kMIDIPropertyIsEmbeddedEntity);
			// Integer isBroadcast = intProperty(peer, resolver, dev,
			// CoreMidiProperty.kMIDIPropertyIsBroadcast);

			Integer singleRealtimeEntity = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertySingleRealtimeEntity);

			// this one is not reliable, as it might be a CFDataRef
			// Integer connectionUniqueID = intProperty(peer, resolver, dev,
			// CoreMidiProperty.kMIDIPropertyConnectionUniqueID);

			Integer offline = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyOffline);
			Integer isPrivate = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyPrivate);
			String driverOwner = stringProperty(cf, peer, resolver, dev, CoreMidiProperty.kMIDIPropertyDriverOwner);
			// CoreMidiProperty.kMIDIPropertyNameConfiguration);
			String image = stringProperty(cf, peer, resolver, dev, CoreMidiProperty.kMIDIPropertyImage);
			Integer driverVersion = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyDriverVersion);
			Integer supportsGeneralMIDI = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertySupportsGeneralMIDI);
			Integer supportsMMC = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertySupportsMMC);
			Integer canRoute = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyCanRoute);
			Integer receivesClock = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyReceivesClock);
			Integer receivesMTC = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyReceivesMTC);
			Integer receivesNotes = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyReceivesNotes);
			Integer receivesProgramChanges = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyReceivesProgramChanges);
			Integer receivesBankSelectMSB = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyReceivesBankSelectMSB);
			Integer receivesBankSelectLSB = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyReceivesBankSelectLSB);
			Integer transmitsClock = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyTransmitsClock);
			Integer transmitsMTC = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyTransmitsMTC);
			Integer transmitsNotes = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyTransmitsNotes);
			Integer transmitsProgramChanges = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyTransmitsProgramChanges);
			Integer transmitsBankSelectMSB = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyTransmitsBankSelectMSB);
			Integer transmitsBankSelectLSB = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyTransmitsBankSelectLSB);
			Integer panDisruptsStereo = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyPanDisruptsStereo);
			Integer isSampler = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyIsSampler);
			Integer isDrumMachine = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyIsDrumMachine);
			Integer isMixer = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyIsMixer);
			Integer isEffectUnit = intProperty(peer, resolver, dev, CoreMidiProperty.kMIDIPropertyIsEffectUnit);
			Integer maxReceiveChannels = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyMaxReceiveChannels);
			Integer maxTransmitChannels = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyMaxTransmitChannels);
			String driverDeviceEditorApp = stringProperty(cf, peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertyDriverDeviceEditorApp);
			Integer supportsShowControl = intProperty(peer, resolver, dev,
					CoreMidiProperty.kMIDIPropertySupportsShowControl);
			String displayName = stringProperty(cf, peer, resolver, dev, CoreMidiProperty.kMIDIPropertyDisplayName);

			System.out.printf("Device %s:%n", dev);
			System.out.printf("  Name: %s%n", name);
			System.out.printf("  Manufacturer: %s%n", manufacturer);
			System.out.printf("  Model: %s%n", model);
			System.out.printf("  UniqueID: %d%n", uniqueId);
			System.out.printf("  DeviceID: %d%n", deviceId);

			IntByReference findByUniqueIdRef = new IntByReference();
			IntByReference findByUniqueIdType = new IntByReference();
			int result = peer.MIDIObjectFindByUniqueID(uniqueId, findByUniqueIdRef, findByUniqueIdType);
			System.out.printf("  Result of MIDIObjectFindByUniqueID: %d%n", result);
			System.out.printf("  MIDIObjectFindByUniqueID dev: %d%n", findByUniqueIdRef.getValue());
			System.out.printf("  MIDIObjectFindByUniqueID type: %s%n",
					MIDIObjectType.byValue(findByUniqueIdType.getValue()));

			// these are endpoint-only
			// System.out.printf(" ReceiveChannels: %d%n", receiveChannels);
			// System.out.printf(" TransmitChannels: %d%n", transmitChannels);

			System.out.printf("  MaxSysExSpeed: %d%n", maxSysExSpeed);
			System.out.printf("  AdvanceScheduleTimeMuSec: %d%n", advanceScheduleTimeMuSec);

			// endpoint only
			// System.out.printf(" IsEmbeddedEntity: %d%n", isEmbeddedEntity);
			// System.out.printf(" IsBroadcast: %d%n", isBroadcast);

			System.out.printf("  SingleRealtimeEntity: %d%n", singleRealtimeEntity);
			// connectionUniqueID
			System.out.printf("  Offline: %d%n", offline);
			System.out.printf("  Private: %d%n", isPrivate);
			System.out.printf("  DriverOwner: %s%n", driverOwner);
			// System.out.printf(" NameConfiguration: %s%n", nameConfiguration);
			System.out.printf("  Image: %s%n", image);
			System.out.printf("  DriverVersion: %d%n", driverVersion);
			System.out.printf("  SupportsGeneralMIDI: %d%n", supportsGeneralMIDI);
			System.out.printf("  SupportsMMC: %d%n", supportsMMC);
			System.out.printf("  CanRoute: %d%n", canRoute);
			System.out.printf("  ReceivesClock: %d%n", receivesClock);
			System.out.printf("  ReceivesMTC: %d%n", receivesMTC);
			System.out.printf("  ReceivesNotes: %d%n", receivesNotes);
			System.out.printf("  ReceivesProgramChanges: %d%n", receivesProgramChanges);
			System.out.printf("  ReceivesBankSelectMSB: %d%n", receivesBankSelectMSB);
			System.out.printf("  ReceivesBankSelectLSB: %d%n", receivesBankSelectLSB);
			System.out.printf("  TransmitsClock: %d%n", transmitsClock);
			System.out.printf("  TransmitsMTC: %d%n", transmitsMTC);
			System.out.printf("  TransmitsNotes: %d%n", transmitsNotes);
			System.out.printf("  TransmitsProgramChanges: %d%n", transmitsProgramChanges);
			System.out.printf("  TransmitsBankSelectMSB: %d%n", transmitsBankSelectMSB);
			System.out.printf("  TransmitsBankSelectLSB: %d%n", transmitsBankSelectLSB);
			System.out.printf("  PanDisruptsStereo: %d%n", panDisruptsStereo);
			System.out.printf("  IsSampler: %d%n", isSampler);
			System.out.printf("  IsDrumMachine: %d%n", isDrumMachine);
			System.out.printf("  IsMixer: %d%n", isMixer);
			System.out.printf("  IsEffectUnit: %d%n", isEffectUnit);
			System.out.printf("  MaxReceiveChannels: %d%n", maxReceiveChannels);
			System.out.printf("  MaxTransmitChannels: %d%n", maxTransmitChannels);
			System.out.printf("  DriverDeviceEditorApp: %s%n", driverDeviceEditorApp);
			System.out.printf("  SupportsShowControl: %d%n", supportsShowControl);
			System.out.printf("  DisplayName: %s%n", displayName);

			System.out.println();
		}
	}

	protected Integer intProperty(
			CoreMidiPeer peer,
			CoreMidiPropertyResolver resolver,
			int device,
			CoreMidiProperty property) {
		CFStringRef propRef = property.resolve(resolver);

		Memory m = new Memory(4);
		int status = peer().MIDIObjectGetIntegerProperty(device, propRef, m);
		CoreMidiError error = CoreMidiError.byErrorCode(status);
		if (error == CoreMidiError.kMIDIUnknownProperty) {
			return null;
		} else if (error != CoreMidiError.kNoError) {
			throw CoreMidiException.fromError(error.errorCode());
		}
		return m.getInt(0);
	}

	protected String stringProperty(
			CoreFoundationPeer cf,
			CoreMidiPeer peer,
			CoreMidiPropertyResolver resolver,
			int device,
			CoreMidiProperty property) {
		CFStringRef propRef = property.resolve(resolver);

		Memory m = new Memory(8);
		int status = peer().MIDIObjectGetStringProperty(device, propRef, m);
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