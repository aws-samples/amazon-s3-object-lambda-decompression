import common_functions

import zstandard as zstd


def decompress(event, context):
    decompressor = zstd.ZstdDecompressor()
    return common_functions.decompress(event, decompressor.stream_reader)
