import requests
import unittest
import random
from unittest.mock import patch
from parameterized import parameterized

import common_functions

EVENT = {
    'getObjectContext': {
        'outputRoute': 'route',
        'outputToken': 'token',
        'inputS3Url': 'http://s3.amazonaws.com/'
    }
}


class BaseTest(unittest.TestCase):

    def get_compression(self, content):
        pass

    def get_decompression_handler(self, event, context):
        pass

    @classmethod
    def setUpClass(cls):
        if cls is BaseTest:
            raise unittest.SkipTest("Skip BaseTest tests, it's a base class")
        super(BaseTest, cls).setUpClass()

    @parameterized.expand([
        [500], [401], [400], [411], [304], [404], [409], [403], [206], [405], [416], [412], [503], [599]])
    @patch("common_functions._get_s3_client")
    @patch("requests.get")
    def test_error_from_s3(self, status_code, mock_get, mock_s3):
        mock_get.return_value.status_code = status_code

        def assert_get_decompression_handler(**kwargs):
            self.assertEqual(kwargs.get("RequestRoute"), "route")
            self.assertEqual(kwargs.get("RequestToken"), "token")
            self.assertEqual(kwargs.get("StatusCode"), status_code)

        mock_s3.return_value.write_get_object_response.side_effect = assert_get_decompression_handler

        result = self.get_decompression_handler(EVENT, None)

        self.assertIsNone(result)

        mock_s3.return_value.write_get_object_response.assert_called_once()

    @patch("common_functions._get_s3_client")
    @patch("requests.get")
    def test_with_dummy_text(self, mock_get, mock_s3):
        dummy_string = "This is some dummy text"
        dummy_content = bytes(dummy_string, "utf-8")

        data = self.get_compression(dummy_content)

        mock_get.return_value.status_code = 200
        mock_get.return_value.headers = {}
        mock_get.return_value.raw = data

        def assert_get_decompression_handler(**kwargs):
            self.assertEqual(kwargs.get("Body").read(1024), dummy_content)
            self.assertEqual(kwargs.get("RequestRoute"), "route")
            self.assertEqual(kwargs.get("RequestToken"), "token")
            self.assertEqual(kwargs.get("Metadata"), {})

        mock_s3.return_value.write_get_object_response.side_effect = assert_get_decompression_handler

        result = self.get_decompression_handler(EVENT, None)

        self.assertIsNone(result)
        mock_s3.return_value.write_get_object_response.assert_called_once()


    @parameterized.expand([
        ['ErrorMessage', 'some_value', 'x-amz-fwd-error-message', 'some_value'],
        ['ContentLanguage', 'some_value', 'Content-Language', 'some_value'],
        ['MissingMeta', 'some_value', 'x-amz-missing-meta', 'some_value'],
        ['ObjectLockLegalHoldStatus', 'some_value', 'x-amz-object-lock-legal-hold', 'some_value'],
        ['ReplicationStatus', 'some_value', 'x-amz-replication-status', 'some_value'],
        ['RequestCharged', 'some_value', 'x-fwd-header-x-amz-request-charged', 'some_value'],
        ['Restore', 'some_value', 'x-amz-restore', 'some_value'],
        ['ServerSideEncryption', 'some_value', 'x-amz-server-side-encryption', 'some_value'],
        ['SSECustomerAlgorithm', 'some_value', 'x-amz-server-side-encryption-customer-algorithm', 'some_value'],
        ['SSEKMSKeyId', 'some_value', 'x-amz-server-side-encryption-aws-kms-key-id', 'some_value'],
        ['SSECustomerKeyMD5', 'some_value', 'x-amz-server-side-encryption-customer-key-MD5', 'some_value'],
        ['StorageClass', 'some_value', 'x-amz-storage-class', 'some_value'],
        ['TagCount', 5, 'x-amz-tagging-count', '5'],
        ['VersionId', 'some_value', 'x-amz-version-id', 'some_value'],
        ['Metadata', {'test-a': 'some_value'}, 'x-amz-meta-test-a', 'some_value']
    ])
    @patch("common_functions._get_s3_client")
    @patch("requests.get")
    def test_return_with_headers(self, param_name, param_value, header, header_value, mock_get, mock_s3):
        dummy_string = "This is some dummy text"
        dummy_content = bytes(dummy_string, "utf-8")

        data = self.get_compression(dummy_content)

        mock_get.return_value.status_code = 200
        mock_get.return_value.headers = {header: header_value}
        mock_get.return_value.raw = data


        def assert_get_decompression_handler(**kwargs):
            self.assertEqual(kwargs.get("Body").read(1024), dummy_content)
            self.assertEqual(kwargs.get("RequestRoute"), "route")
            self.assertEqual(kwargs.get("RequestToken"), "token")
            self.assertEqual(kwargs.get(param_name), param_value)

        mock_s3.return_value.write_get_object_response.side_effect = assert_get_decompression_handler

        result = self.get_decompression_handler(EVENT, None)

        self.assertIsNone(result)
        mock_s3.return_value.write_get_object_response.assert_called_once()

    @patch("common_functions._get_s3_client")
    @patch("requests.get")
    def test_error_retrieving_data(self, mock_get, mock_s3):
        mock_get.side_effect = requests.exceptions.RequestException("Some error")

        # mock_get.side_effect = MockGetResponse

        def assert_get_decompression_handler(**kwargs):
            self.assertEqual(kwargs.get("StatusCode"), 500)
            self.assertEqual(kwargs.get("RequestRoute"), "route")
            self.assertEqual(kwargs.get("RequestToken"), "token")

        mock_s3.return_value.write_get_object_response.side_effect = assert_get_decompression_handler

        result = self.get_decompression_handler(EVENT, None)

        self.assertIsNone(result)
        mock_s3.return_value.write_get_object_response.assert_called_once()
