import hashlib


class ChecksumMismatchError(Exception):
    pass


class ChecksumChecker:
    """ChecksumChecker is checking checksum of the processed data
    to detect in memory corruption (e. g. bit flip)"""

    def __init__(self, source_stream, etag=None):
        self.body = source_stream
        self.etag = etag
        self._md5Processed = hashlib.md5()
        self._md5NotProcessed = hashlib.md5()
        self._data = None

    def read(self, size=None):
        if self._data:
            self._md5Processed.update(self._data)
            if self._md5Processed.digest() != self._md5NotProcessed.digest():
                raise ChecksumMismatchError()

        self._data = self.body.read(size)
        self._md5NotProcessed.update(self._data)

        if len(self._data) == 0:
            if self.etag and self.etag != self._md5Processed.hexdigest():
                raise ChecksumMismatchError()

        return self._data

    def __iter__(self):
        r = self.read(1048576)
        while r:
            yield r
            r = self.read(1048576)