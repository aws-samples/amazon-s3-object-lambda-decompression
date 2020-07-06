build-DecompressSnappyFunction:

	pip install --upgrade pip

	pip install cffi

	yum -y update && yum -y install \
    gcc \
    gcc72-c++ \
    zip \
    snappy \
    snappy-devel \
    yum-utils \
    rpmdevtools \
    && yum clean all

	cd /tmp && \
	yumdownloader snappy snappy-devel && \
	rpmdev-extract *rpm

	-cp /tmp/*/usr/lib64/* $(ARTIFACTS_DIR)

	cd $(ARTIFACTS_DIR) && \
	cp -R -t . /tmp/samcli/source/{lambda_function.py,tests,requirements.txt} && \
	pip install -r requirements.txt --target .

