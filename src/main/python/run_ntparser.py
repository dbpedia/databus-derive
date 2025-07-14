import os
import glob
import argparse
import jpype
import jpype.imports
from jpype.types import *

def main():
    parser = argparse.ArgumentParser(description="Run NTParser with given arguments.")
    parser.add_argument("--input", required=True, help="Path to input .bz2 RDF file")
    parser.add_argument("--triples", required=True, help="Path to output triples .nt file")
    parser.add_argument("--report", required=True, help="Path to output report file")
    parser.add_argument("--chunk", type=int, default=10000, help="Number of triples per chunk")
    parser.add_argument("--reportFormat", choices=["TEXT", "JSON"], default="TEXT", help="Report format (TEXT or JSON)")
    parser.add_argument("--removeWarnings", action="store_true", help="Remove warnings in output")

    args = parser.parse_args()

    base_dir = os.path.dirname(os.path.abspath(__file__))
    jar_glob = os.path.join(base_dir, "../../../target/*.jar")
    jar_files = glob.glob(jar_glob)

    if not jar_files:
        raise FileNotFoundError(f".jar-File not found under {jar_glob}")
    jar_path = jar_files[0]

    if not jpype.isJVMStarted():
        jpype.startJVM(classpath=[jar_path])

    from org.dbpedia.utils.parser import NTParser

    try:
        print(f"Parsing RDF data from {args.input}...")
        NTParser.parse(
            args.input,
            args.triples,
            args.report,
            args.chunk,
            args.reportFormat,
            args.removeWarnings
        )
        print("Parsing complete.")
        print(f"  Triples: {args.triples}")
        print(f"  Report:  {args.report}")

    except Exception as ex:
        print("Error while parsing RDF data:")
        import traceback
        traceback.print_exc()

    finally:
        if jpype.isJVMStarted():
            jpype.shutdownJVM()


if __name__ == "__main__":
    main()
