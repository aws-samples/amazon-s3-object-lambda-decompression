import bz2
import common_functions


def decompress(event, context):
    return common_functions.decompress(event, lambda data: bz2.BZ2File(data, mode="rb"))
