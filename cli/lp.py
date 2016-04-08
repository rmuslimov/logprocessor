#! coding: utf-8
"""
Usage:
  lp-cli.py <url> <level> <app> <year> <month> [<day>]

Examples:
  python cli/lp.py lf:7800/tasks bcd1 fokker 2016 2 2
  python cli/lp.py lf:7800/tasks bcd1 fokker 2016 2

  python cli/lp.py search.team.getgoing.com:7800/tasks bcd1 fokker 2016 4 7
"""

import json

from docopt import docopt

import requests


def main(args):
    payload = {
        'app': args['<app>'],
        'level': args['<level>'],
        'month': args['<month>'],
        'year': args['<year>']
    }
    url = args['<url>']
    if not url.startswith('http'):
        url = 'http://' + url

    if args['<day>']:
        payload['day'] = args['<day>']

    rq = requests.put(url, data=payload)
    if rq.status_code == 201:
        data = json.loads(rq.content)
        print 'Task started: {0}'.format(data.get('task-id'))
    else:
        print 'Task adding failed. status_code={}, content={}'.format(
            rq.status_code, rq.content)


if __name__ == '__main__':
    main(docopt(__doc__))
