
import os

from datetime import datetime
from lxml import etree


def import_file(filename):
    with open(filename) as f:
        body = etree.fromstring(f.read())

    return etree.ETXPath(
        './/{http://schemas.xmlsoap.org/soap/envelope/}Body')(body)


def walk(path):
    return [
        import_file(os.path.join(path, each)) for each
        in os.listdir(path)
    ]


def main():
    t = datetime.now()
    walk('/Users/rmuslimov/projects/logs/d=12')
    print 'Time elapsed: {} sec'.format(datetime.now() - t)

for i in range(10):
    main()
