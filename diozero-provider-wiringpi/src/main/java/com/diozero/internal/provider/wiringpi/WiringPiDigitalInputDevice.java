package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Device I/O Zero - wiringPi provider
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterruptCallback;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDigitalInputDevice extends AbstractInputDevice<DigitalPinEvent>
implements GpioDigitalInputDeviceInterface, GpioInterruptCallback {
	private int pinNumber;
	private int edge;
	
	public WiringPiDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			GpioPullUpDown pud, GpioEventTrigger trigger) throws RuntimeIOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		switch (trigger) {
		case RISING:
			edge = Gpio.INT_EDGE_RISING;
			break;
		case FALLING:
			edge = Gpio.INT_EDGE_FALLING;
			break;
		case BOTH:
		default:
			edge = Gpio.INT_EDGE_BOTH;
			break;
		}
		
		try {
			// Note calling this method will automatically export the pin and set the pin direction to INPUT
			if (!GpioUtil.setEdgeDetection(pinNumber, edge)) {
				throw new RuntimeIOException("Error setting edge detection (" + edge + ") for pin " + pinNumber);
			}
		} catch (RuntimeException re) {
			throw new RuntimeIOException(re);
		}
	
		int wpi_pud;
		switch (pud) {
		case PULL_DOWN:
			wpi_pud = Gpio.PUD_DOWN;
			break;
		case PULL_UP:
			wpi_pud = Gpio.PUD_UP;
			break;
		case NONE:
		default:
			wpi_pud = Gpio.PUD_OFF;
			break;
		}
		Gpio.pullUpDnControl(pinNumber, wpi_pud);
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		removeListener();
		GpioUtil.unexport(pinNumber);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return Gpio.digitalRead(pinNumber) == 1;
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Support software debounce...
	}

	@Override
	public void enableListener() {
		// TODO Validate that wiringPi actually works this way, i.e. supports separate callbacks for rising and falling
		// Note it looks easier to use GpioInterrupt but this will be less efficient
		// due to its use of Vectors to support multiple listeners and cloning on every event
		Gpio.wiringPiISR(pinNumber, edge, this);
	}

	@Override
	public void disableListener() {
		// TODO Is this correct?
		Gpio.wiringPiISR(pinNumber, edge, null);
	}

	@Override
	public void callback(int pin) {
		long nano_time = System.nanoTime();
		try {
			// Really don't like that the callback doesn't include the value, never mind the timestamps
			valueChanged(new DigitalPinEvent(pin, System.currentTimeMillis(), nano_time, getValue()));
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error invoking getValue(): {}", e);
		}
	}
}