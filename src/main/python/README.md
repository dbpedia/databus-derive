## Setup
```bash
pip install -r requirements.txt
mvn clean package -f ../../../pom.xml
```

## Example Call
```bash
python run_ntparser.py \
  --input /home/theo/Work/SCADS.AI/Projects/databus-derive/ntfiles/test.nt.bz2 \
  --triples /home/theo/Work/SCADS.AI/Projects/databus-derive/output-triples.nt \
  --report /home/theo/Work/SCADS.AI/Projects/databus-derive/output-report.txt \
  --chunk 5000 \
  --reportFormat TEXT \
  --removeWarnings

```