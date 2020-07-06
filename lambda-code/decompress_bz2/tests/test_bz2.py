import bz2
import io
import unittest

import lambda_function
from common_test_functions import BaseTest


class Bz2Test(BaseTest):

    def get_decompression_handler(self, event, context):
        return lambda_function.decompress(event, context)

    def get_compression(self, content):
        data = io.BytesIO()
        with bz2.BZ2File(data, "wb") as f:
            f.write(content)
        data.seek(0)
        return data


if __name__ == '__main__':
    unittest.main()
