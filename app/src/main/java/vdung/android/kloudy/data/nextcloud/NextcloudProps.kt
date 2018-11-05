package vdung.android.kloudy.data.nextcloud

import org.xmlpull.v1.XmlPullParser
import vdung.kodav.Prop
import vdung.kodav.Xml

data class FileId(override val value: Int?) : Prop<Int> {
    companion object : Prop.Parser<FileId> {
        override val tag = Xml.Tag("http://owncloud.org/ns", "fileid")

        override fun parse(parser: XmlPullParser) = FileId(Xml.parseText(parser)?.toInt())
    }
}
