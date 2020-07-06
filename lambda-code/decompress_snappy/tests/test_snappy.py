import io
import unittest
import stream_decompressor
import snappy

import lambda_function
from common_test_functions import BaseTest


class SnappyTest(BaseTest):

    def get_decompression_handler(self, event, context):
        return lambda_function.decompress(event, context)

    def get_compression(self, content):
        data = io.BytesIO()
        snappy.stream_compress(io.BytesIO(content), data)
        data.seek(0)
        return data


if __name__ == '__main__':
    unittest.main()
