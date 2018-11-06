package vdung.android.kloudy.data

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable


fun <T : Parcelable> T.marshall(): ByteArray {
    val parcel = Parcel.obtain()
    Bundle().let {
        it.putParcelable(this::class.java.name, this)
        parcel.writeBundle(it)
    }

    return parcel.marshall().also {
        parcel.recycle()
    }
}

inline fun <reified T : Parcelable> unmarshall(bytes: ByteArray): T? {
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    return parcel.readBundle(T::class.java.classLoader).also {
        parcel.recycle()
    }?.getParcelable(T::class.java.name)
}