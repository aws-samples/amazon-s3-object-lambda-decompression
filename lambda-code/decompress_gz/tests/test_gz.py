import io
import unittest
import gzip

import lambda_function
from common_test_functions import BaseTest


class GzTest(BaseTest):

    def get_decompression_handler(self, event, context):
        return lambda_function.decompress(event, context)

    def get_compression(self, content):
        data = io.BytesIO()
        with gzip.GzipFile(fileobj=data, mode="wb") as f:
            f.write(content)
        data.seek(0)
        return data


if __name__ == '__main__':
    unittest.main()
