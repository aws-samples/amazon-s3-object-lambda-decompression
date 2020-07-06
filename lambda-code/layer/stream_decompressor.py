class StreamDecompressor:
    def __init__(self, compressed_file_obj, decompressor):
        self.data = StreamDecompressor._decompressor_chunk_gen(compressed_file_obj, decompressor)

    def read(self, _len):
        for d in self.data:
            return d

    @staticmethod
    def _decompressor_chunk_gen(compressed_file_obj, decompressor):
        """This function is used for the snappy and zlib methods only"""
        while True:
            compressed_chunk = compressed_file_obj.read(4096)

            # If end of file reached
            if not compressed_chunk:
                break

            decompressed = decompressor.decompress(compressed_chunk)

            # Need to make sure we don't send empty chunks, could close connection
            if decompressed:
                yield decompressed

        yield decompressor.flush()