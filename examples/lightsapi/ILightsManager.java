package com.example.halolite.lightsapi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ILightsManager extends IInterface {
    String DESCRIPTOR = "miui.lights.ILightsManager";

    void setColorfulLight(String pkg, int styleType, int userId) throws RemoteException;
    void setColorCommon(int color, String pkg, int styleType, int userId) throws RemoteException;
    void setColorLed(int color, String pkg, int styleType, int userId, int category) throws RemoteException;
    void setCustomLight(int color, int flashMode, int onMs, int offMs, int brightNessMode, String pkg, int styleType, int userId) throws RemoteException;

    abstract class Stub extends Binder implements ILightsManager {
        public Stub() { attachInterface(this, DESCRIPTOR); }

        public static ILightsManager asInterface(IBinder obj) {
            if (obj == null) return null;
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof ILightsManager) return (ILightsManager) iin;
            return new Proxy(obj);
        }

        @Override public IBinder asBinder() { return this; }
        @Override public boolean onTransact(int code, Parcel data, Parcel reply, int flags) { return false; }

        private static class Proxy implements ILightsManager {
            private final IBinder mRemote;
            Proxy(IBinder remote) { this.mRemote = remote; }
            @Override public IBinder asBinder() { return mRemote; }

            @Override
            public void setColorfulLight(String pkg, int styleType, int userId) throws RemoteException {
                Parcel data = Parcel.obtain(); Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(pkg); data.writeInt(styleType); data.writeInt(userId);
                    mRemote.transact(1, data, reply, 0);
                    reply.readException();
                } finally { reply.recycle(); data.recycle(); }
            }

            @Override
            public void setColorCommon(int color, String pkg, int styleType, int userId) throws RemoteException {
                Parcel data = Parcel.obtain(); Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(color); data.writeString(pkg); data.writeInt(styleType); data.writeInt(userId);
                    mRemote.transact(2, data, reply, 0);
                    reply.readException();
                } finally { reply.recycle(); data.recycle(); }
            }

            @Override
            public void setColorLed(int color, String pkg, int styleType, int userId, int category) throws RemoteException {
                Parcel data = Parcel.obtain(); Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(color); data.writeString(pkg); data.writeInt(styleType);
                    data.writeInt(userId); data.writeInt(category);
                    mRemote.transact(3, data, reply, 0);
                    reply.readException();
                } finally { reply.recycle(); data.recycle(); }
            }

            @Override
            public void setCustomLight(int color, int flashMode, int onMs, int offMs, int brightNessMode, String pkg, int styleType, int userId) throws RemoteException {
                Parcel data = Parcel.obtain(); Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(color); data.writeInt(flashMode); data.writeInt(onMs);
                    data.writeInt(offMs); data.writeInt(brightNessMode); data.writeString(pkg);
                    data.writeInt(styleType); data.writeInt(userId);
                    mRemote.transact(4, data, reply, 0);
                    reply.readException();
                } finally { reply.recycle(); data.recycle(); }
            }
        }
    }
}
