	@rem set JCOPTS=-Xlint:unchecked
	@set JCOPTS=-g:none -nowarn
	@set JC=javac -cp ".;*"
	@set APP=AppServer
	@set USR_ROOTDIR=c:\apps\usr\apo
	@set USR_ADMINDIR=c:\apps\usr\apo\admin
	@set SRC_DIR=com\apo\apps\%APP%
	@set JAR_FILE=%APP%.jar

	cd %USR_ADMINDIR%
	java -jar %JAR_FILE% flipper loopback -port:8484 -d -mode:rox
		@if not %ERRORLEVEL%==0 pause
	@if not "%1" == "" goto heaven

:heaven
	@rem echo contents of %JAR_FILE%...
	@rem jar tvf %JAR_FILE%
	@echo Heaven!
	@goto end

:hell
	@echo Error: %ERRORLEVEL%
	@pause
	@goto end

:end
