build-DecompressZstdFunction:
	yum -y update && yum -y install gcc gcc-c++ python-devel

	cd $(ARTIFACTS_DIR) && \
	cp -R -t . /tmp/samcli/source/{lambda_function.py,tests,requirements.txt} && \
	pip install -r requirements.txt --target .