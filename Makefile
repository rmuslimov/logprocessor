.PHONY: autotest


autotest:
	lein midje :autotest :filter -slow
