import gzip
import common_functions

def decompress(event, context):
    return common_functions.decompress(event, lambda data:gzip.GzipFile(fileobj=data,  mode="rb"))
