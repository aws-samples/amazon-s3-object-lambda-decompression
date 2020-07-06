import zlib
import io
import unittest

import lambda_function
from common_test_functions import BaseTest


class ZlibTest(BaseTest):

    def get_decompression_handler(self, event, context):
        return lambda_function.decompress(event, context)

    def get_compression(self, content):
        data = io.BytesIO()
        data.write(zlib.compress(content, 2))
        data.seek(0)
        return data


if __name__ == '__main__':
    unittest.main()
