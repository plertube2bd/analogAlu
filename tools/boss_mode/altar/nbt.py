"""Minimal NBT (de)serializer used to read vanilla structure templates and write the Shard Altar template."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Tag:
    """Explicitly typed value, for when the Python type alone is ambiguous (byte/short/long/float)."""

    def __init__(self, kind, value):
        self.kind, self.value = kind, value

    def __repr__(self):
        return f"Tag({self.kind}, {self.value!r})"


class TypedList(list):
    """List carrying its element tag type (needed for empty lists)."""

    def __init__(self, kind, items=()):
        super().__init__(items)
        self.kind = kind


def _read(buf, pos, kind):
    if kind == BYTE:
        return struct.unpack_from(">b", buf, pos)[0], pos + 1
    if kind == SHORT:
        return struct.unpack_from(">h", buf, pos)[0], pos + 2
    if kind == INT:
        return struct.unpack_from(">i", buf, pos)[0], pos + 4
    if kind == LONG:
        return struct.unpack_from(">q", buf, pos)[0], pos + 8
    if kind == FLOAT:
        return struct.unpack_from(">f", buf, pos)[0], pos + 4
    if kind == DOUBLE:
        return struct.unpack_from(">d", buf, pos)[0], pos + 8
    if kind == STRING:
        n = struct.unpack_from(">H", buf, pos)[0]
        return buf[pos + 2:pos + 2 + n].decode("utf-8"), pos + 2 + n
    if kind in (BYTE_ARRAY, INT_ARRAY, LONG_ARRAY):
        n = struct.unpack_from(">i", buf, pos)[0]
        fmt, size = {BYTE_ARRAY: ("b", 1), INT_ARRAY: ("i", 4), LONG_ARRAY: ("q", 8)}[kind]
        return list(struct.unpack_from(f">{n}{fmt}", buf, pos + 4)), pos + 4 + n * size
    if kind == LIST:
        elem, n = struct.unpack_from(">bi", buf, pos)
        pos += 5
        out = TypedList(elem)
        for _ in range(n):
            v, pos = _read(buf, pos, elem)
            out.append(v)
        return out, pos
    if kind == COMPOUND:
        out = {}
        while True:
            t = buf[pos]
            pos += 1
            if t == END:
                return out, pos
            name, pos = _read(buf, pos, STRING)
            out[name], pos = _read(buf, pos, t)
    raise ValueError(kind)


def load(path):
    data = open(path, "rb").read()
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    assert data[0] == COMPOUND
    _, pos = _read(data, 1, STRING)
    value, _ = _read(data, pos, COMPOUND)
    return value


def _kind(v):
    if isinstance(v, Tag):
        return v.kind
    if isinstance(v, bool):
        return BYTE
    if isinstance(v, int):
        return INT
    if isinstance(v, float):
        return DOUBLE
    if isinstance(v, str):
        return STRING
    if isinstance(v, dict):
        return COMPOUND
    if isinstance(v, list):
        return LIST
    raise TypeError(type(v))


def _write(out, kind, v):
    if isinstance(v, Tag):
        v = v.value
    if kind == BYTE:
        out += struct.pack(">b", int(v))
    elif kind == SHORT:
        out += struct.pack(">h", v)
    elif kind == INT:
        out += struct.pack(">i", v)
    elif kind == LONG:
        out += struct.pack(">q", v)
    elif kind == FLOAT:
        out += struct.pack(">f", v)
    elif kind == DOUBLE:
        out += struct.pack(">d", v)
    elif kind == STRING:
        b = v.encode("utf-8")
        out += struct.pack(">H", len(b)) + b
    elif kind == LIST:
        elem = v.kind if isinstance(v, TypedList) else (_kind(v[0]) if v else END)
        out += struct.pack(">bi", elem, len(v))
        for item in v:
            _write(out, elem, item)
    elif kind == COMPOUND:
        for name, item in v.items():
            k = _kind(item)
            out += struct.pack(">b", k)
            _write(out, STRING, name)
            _write(out, k, item)
        out += b"\x00"
    elif kind == INT_ARRAY:
        out += struct.pack(f">i{len(v)}i", len(v), *v)
    else:
        raise ValueError(kind)


def save(path, value):
    out = bytearray(b"\x0a")
    _write(out, STRING, "")
    _write(out, COMPOUND, value)
    with open(path, "wb") as f:
        f.write(gzip.compress(bytes(out)))
