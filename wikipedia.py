import sys
import time
import urllib.parse
from datetime import datetime
from xml.etree import ElementTree as ET

import requests
from bs4 import BeautifulSoup


def snooze():
    time.sleep(2)


def page_history(page, offset=''):
    print(f'{offset}.. ', end='', file=sys.stderr, flush=True)

    rvlimit = 500  # revisions per page
    url = f'http://en.wikipedia.org/w/api.php?action=query&prop=revisions&titles={urllib.parse.quote(page)}&rvprop=timestamp|user|size&rvlimit={rvlimit}&format=xml'
    if offset:
        url += f'&rvstartid={offset}'
    snooze()
    print(url)

    url = f'https://asoiaf.huijiwiki.com/api.php?action=query&prop=revisions&titles=%E8%89%BE%E5%BE%B7%C2%B7%E5%8F%B2%E5%A1%94%E5%85%8B&rvprop=timestamp|user|size&rvlimit=500&format=xml'
    time.sleep(0.5)  # easy
    agent = requests.Session()
    agent.headers.update({'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36'})
    doc = ET.fromstring(agent.get(url).text)
    print(doc)
    revisions = [
        {
            'filename': page,
            'date': int(time.mktime(time.strptime(rev.attrib['timestamp'], '%Y-%m-%dT%H:%M:%SZ'))) * 1000,
            'author': rev.attrib['user'],
            'weight': max(int(int(rev.attrib['size']) / 100) + 1, 1)
        }
        for rev in doc.iter('rev')
    ]

    rvstartid = doc.find('.//query-continue/revisions')
    if rvstartid is not None:
        rvstartid = rvstartid.attrib.get('rvstartid', None)
    if rvstartid:
        revisions += page_history(page, rvstartid) or []
    return revisions


def user_history(username, offset=''):
    rvlimit = 500  # revisions per page
    url = f'http://en.wikipedia.org/w/index.php?title=Special:Contributions&limit={rvlimit}&target={username}'
    if offset:
        url += f'&offset={offset}'
    agent = requests.Session()
    agent.headers.update({'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36'})
    snooze()
    doc = BeautifulSoup(agent.get(url).text, 'html.parser')
    revisions = [
        {
            'filename': li.select('a')[2].get_text(),
            'date': int(time.mktime(time.strptime(li.get_text().split('(')[0].strip(), '%H:%M, %d %B %Y'))) * 1000,
            'author': username,
            'weight': 1
        }
        for li in doc.select('#bodyContent li')
    ]

    link = doc.select_one('.mw-nextlink')
    if link:
        rvstartid = link['href'].split('offset=')[-1].split('&')[0]
        revisions += user_history(username, rvstartid) or []
    return revisions

# parse inputs
if len(sys.argv) < 2 or sys.argv[1] == '':
    print(f"{sys.argv[0]}: specify page(s) as parameters (remember to quote 'Barack Obama')", file=sys.stderr)
    sys.exit(1)
pages = sys.argv[1:]

print(f"Building revhistory for {pages}...", file=sys.stderr)
revisions = []
for page in pages:
    print(f"\n{page}", file=sys.stderr)
    if page.startswith('User:'):
        revisions += user_history(page)
    else:
        revisions += page_history(page)
print(revisions)
revisions.sort(key=lambda r: r['date'])
# with open("revisions.txt", "w") as file:
#     file.write('<?xml version="1.0"?>')
#     file.write('<file_events>')
#     for rev in revisions:
#         # code_swarm wants unixtime in milliseconds
#         # timestamp = int(datetime.strptime(rev['date'], '%Y-%m-%d %H:%M:%S').timestamp() * 1000)
#         # print(f'<event date="{timestamp}" filename="{rev["filename"]}" author="{rev["author"]}" weight="{rev["weight"]}" />')
#         file.write(f'<event filename="{rev["filename"]}" date="{rev["date"]}" author="{rev["author"]}" weight="{rev["weight"]}" /></event>')
#     file.write('</file_events>')
root = ET.Element("file_events")
for rev in revisions:
    event = ET.SubElement(root, "event")
    event.set("filename", rev["filename"])
    event.set("date", str(rev["date"]))
    event.set("author", rev["author"])
    event.set("weight", str(rev["weight"]))
tree = ET.ElementTree(root)
tree.write("revisions.xml", encoding="utf-8", xml_declaration=True)
sys.exit(0)