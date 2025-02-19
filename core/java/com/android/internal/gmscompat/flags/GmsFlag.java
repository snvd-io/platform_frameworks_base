/*
 * Copyright (C) 2022 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.gmscompat.flags;

import android.annotation.Nullable;
import android.app.compat.gms.GmsCompat;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import com.android.internal.gmscompat.GmsInfo;

import java.util.Map;
import java.util.function.Supplier;

public class GmsFlag implements Parcelable {
    private static final String TAG = "GmsFlag";

    public String name;

    public static final int TYPE_BOOL = 0;
    public static final int TYPE_INT = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_BYTES = 4;
    public byte type;

    public static final int ACTION_SET = 0;
    public static final int ACTION_APPEND = 1; // only for TYPE_STRING
    public byte action;

    public boolean boolArg;
    public long integerArg;
    public double floatArg;
    public String stringArg;
    public byte[] bytesArg;

    public @Nullable Supplier valueSupplier;

    public byte permissionCheckMode;
    public static final int PERMISSION_CHECK_MODE_NONE_OF = 0;
    public static final int PERMISSION_CHECK_MODE_NOT_ALL_OF = 1;
    public static final int PERMISSION_CHECK_MODE_ALL_OF = 2;
    public @Nullable String[] permissions;

    public static final String NAMESPACE_GSERVICES = "gservices";
    // Gservices content provider is hosted by GmsCore since Android 15. It was hosted by GSF before 15.
    public static final String GSERVICES_CONTENT_PROVIDER_AUTHORITY = "com.google.android.gsf.gservices";

    public static final String GSERVICES_URI = "content://"
            + GSERVICES_CONTENT_PROVIDER_AUTHORITY + "/prefix";

    public static final String PHENOTYPE_URI_PREFIX = "content://"
            + GmsInfo.PACKAGE_GMS_CORE + ".phenotype/";

    public GmsFlag() {}

    public GmsFlag(String name) {
        this.name = name;
    }

    private boolean permissionsMatch() {
        String[] perms = permissions;
        if (perms == null) {
            return true;
        }

        int numOfGrantedPermissions = 0;

        for (String perm : perms) {
            if (GmsCompat.hasPermission(perm)) {
                ++numOfGrantedPermissions;
            }
        }

        switch (permissionCheckMode) {
            case PERMISSION_CHECK_MODE_NONE_OF:
                return numOfGrantedPermissions == 0;
            case PERMISSION_CHECK_MODE_NOT_ALL_OF:
                return numOfGrantedPermissions != perms.length;
            case PERMISSION_CHECK_MODE_ALL_OF:
                return numOfGrantedPermissions == perms.length;
            default:
                return false;
        }
    }

    public void applyToGservicesMap(ArrayMap<String, String> map) {
        if (!shouldOverride()) {
            return;
        }

        if (type != TYPE_STRING) {
            // all Gservices flags are Strings
            throw new IllegalStateException();
        }

        maybeOverrideString(map);
    }

    private static final int PHENOTYPE_BASE64_FLAGS = Base64.NO_PADDING | Base64.NO_WRAP;

    public void applyToPhenotypeMap(Map map) {
        if (!shouldOverride()) {
            return;
        }

        if (valueSupplier != null) {
            Object val = valueSupplier.get();

            String s;
            if (type == TYPE_BYTES) {
                s = Base64.encodeToString((byte[]) val, PHENOTYPE_BASE64_FLAGS);
            } else {
                s = val.toString();
            }

            map.put(name, s);
            return;
        }

        String s;
        switch (type) {
            case TYPE_BOOL:
                s = boolArg ? "1" : "0";
                break;
            case TYPE_INT:
                s = Long.toString(integerArg);
                break;
            case TYPE_FLOAT:
                s = Double.toString(floatArg);
                break;
            case TYPE_STRING:
                maybeOverrideString(map);
                return;
            case TYPE_BYTES:
                s = Base64.encodeToString(bytesArg, PHENOTYPE_BASE64_FLAGS);
                break;
            default:
                return;
        }

        map.put(name, s);
    }

    public boolean shouldOverride() {
        if (!permissionsMatch()) {
            return false;
        }

        return true;
    }

    // method names and types match columns in phenotype.db database, tables Flags and FlagOverrides

    public int boolVal(int orig) {
        if (type != TYPE_BOOL) {
            logTypeMismatch();
            return orig;
        }
        if (valueSupplier != null) {
            boolean v = ((Boolean) valueSupplier.get()).booleanValue();
            return v ? 1 : 0;
        }
        return boolArg ? 1 : 0;
    }

    public long intVal(long orig) {
        if (type != TYPE_INT) {
            logTypeMismatch();
            return orig;
        }
        if (valueSupplier != null) {
            return ((Long) valueSupplier.get()).longValue();
        }
        return integerArg;
    }

    public double floatVal(double orig) {
        if (type != TYPE_FLOAT) {
            logTypeMismatch();
            return orig;
        }
        if (valueSupplier != null) {
            return ((Double) valueSupplier.get()).doubleValue();
        }
        return floatArg;
    }

    public void maybeOverrideString(Map map) {
        if (type != TYPE_STRING) {
            logTypeMismatch();
            return;
        }
        if (valueSupplier != null) {
            map.put(name, valueSupplier.get());
            return;
        }

        if (action == ACTION_SET) {
            map.put(name, stringArg);
            return;
        }

        if (action == ACTION_APPEND) {
            if (!map.containsKey(name)) {
                Log.d(TAG, name + " is not present in the map, skipping ACTION_APPEND");
                return;
            }

            Object orig = map.get(name);

            if (!(orig instanceof String)) {
                Log.w(TAG, "original value of " +  name + " is not a string, skipping ACTION_APPEND. Value: " + orig);
                return;
            }

            map.put(name, (String) orig + stringArg);
            return;
        }

        Log.d(TAG, "unknown action " + action + " for " + name);
    }

    public byte[] extensionVal(byte[] orig) {
        if (type != TYPE_BYTES) {
            logTypeMismatch();
            return orig;
        }
        if (valueSupplier != null) {
            return (byte[]) valueSupplier.get();
        }
        return bytesArg;
    }

    public void initAsSetString(String v) {
        type = GmsFlag.TYPE_STRING;
        action = GmsFlag.ACTION_SET;
        stringArg = v;
    }

    private void logTypeMismatch() {
        Log.e(TAG, "type mismatch for key " + name, new Throwable());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(name);
        p.writeByte(type);
        p.writeByte(permissionCheckMode);
        p.writeStringArray(permissions);
        p.writeByte(action);
        p.writeBoolean(boolArg);
        p.writeLong(integerArg);
        p.writeDouble(floatArg);
        p.writeString(stringArg);
        p.writeByteArray(bytesArg);
    }

    public static final Parcelable.Creator<GmsFlag> CREATOR = new Creator<>() {
        @Override
        public GmsFlag createFromParcel(Parcel p) {
            GmsFlag f = new GmsFlag();
            f.name = p.readString();
            f.type = p.readByte();
            f.permissionCheckMode = p.readByte();
            f.permissions = p.readStringArray();
            f.action = p.readByte();
            f.boolArg = p.readBoolean();
            f.integerArg = p.readLong();
            f.floatArg = p.readDouble();
            f.stringArg = p.readString();
            f.bytesArg = p.createByteArray();
            return f;
        }

        @Override
        public GmsFlag[] newArray(int size) {
            return new GmsFlag[size];
        }
    };

    public static void writeMapEntry(ArrayMap<String, GmsFlag> map, int idx, Parcel dst) {
        // map key is GmsFlag.name, do not write it twice
        map.valueAt(idx).writeToParcel(dst, 0);
    }

    public static void readMapEntry(Parcel p, ArrayMap<String, GmsFlag> dst) {
        GmsFlag f = GmsFlag.CREATOR.createFromParcel(p);
        dst.append(f.name, f);
    }
}
