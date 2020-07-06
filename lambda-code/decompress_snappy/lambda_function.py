import common_functions
import snappy
import stream_decompressor


def decompress(event, context):

    return common_functions.decompress(event, lambda data: stream_decompressor.StreamDecompressor(data, snappy.StreamDecompressor()))
