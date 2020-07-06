import common_functions
import zlib

import stream_decompressor


def decompress(event, context):
    return common_functions.decompress(event, lambda data: stream_decompressor.StreamDecompressor(data, zlib.decompressobj()))
