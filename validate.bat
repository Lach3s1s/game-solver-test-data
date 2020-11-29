
@echo off
SET /A ARGS_COUNT=0
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1
@echo on
ECHO Reading %ARGS_COUNT% arguments
@echo off

IF %ARGS_COUNT% == 0 (
    @echo on
    ECHO Missing arguments
    @echo off
) ELSE (
	goto myloop
)


:myloop
REM ECHO start loop: %1
IF "%1" == "" (
    goto myfin
)
@echo on

@echo off 
set str=%1
shift

REM @echo on
REM DIR /b /s %str%
REM @echo off

FOR /F %%G IN ('DIR /b /s %str%') DO (
	call :Foo %%G
)

REM @echo off
goto myloop

:myfin

:Foo
set input=%1

IF "%input%" == "" (
	goto End
)

@echo on
echo input file: %input%


@echo off 
set res=%input:computed=res%
@echo on
echo result file: %res%
@echo off 
	
IF NOT EXIST %res% (
	move %input% %res%
	git add %res%
) ELSE (
	ECHO Do not erase existing file!
)
:End