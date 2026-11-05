"""Remove the EXIF and XMP metadata chunks from a PNG so viewers show its pixels as stored.

Simulator screenshots taken in landscape are saved sideways with an EXIF rotation
flag. After the pixels are rotated upright, the stale flag would make viewers (and
App Store Connect) rotate them again, so the flag has to go.
"""
import struct
import sys

for path in sys.argv[1:]:
    data = open(path, "rb").read()
    signature = data[:8]
    kept = [signature]
    offset = 8
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        chunk_type = data[offset + 4:offset + 8]
        chunk_end = offset + 12 + length
        if chunk_type not in (b"eXIf", b"iTXt"):
            kept.append(data[offset:chunk_end])
        offset = chunk_end
    open(path, "wb").write(b"".join(kept))
