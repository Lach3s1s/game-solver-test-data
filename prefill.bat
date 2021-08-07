
@echo off
SET /A ARGS_COUNT=0
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1
@echo on
ECHO Reading %ARGS_COUNT% arguments
@echo off

SET mydate=%1

IF %ARGS_COUNT% == 0 (
    @echo on
    ECHO Missing arguments
    @echo off
) ELSE (
    IF %ARGS_COUNT% == 1 (
        for %%x in (facile, difficile, diabolique) do (
            IF NOT EXIST sudoku\sudoku_%mydate%2021_%%x.txt (
                copy sudoku_DDMMYYYY_TYPE.txt sudoku\sudoku_%mydate%2021_%%x.txt
            ) ELSE (
                ECHO Do not erase existing file!
            )
        )
    ) ELSE (
        goto myloop
    )
)

:myloop
REM start by shifting to remove first arg (date)
shift
REM ECHO start loop: %1
IF "%1" == "" (
    goto myfin
)
@echo on
IF NOT EXIST sudoku\sudoku_%mydate%2021_%1.txt (
	copy sudoku_DDMMYYYY_TYPE.txt sudoku\sudoku_%mydate%2021_%1.txt
) ELSE (
	ECHO Do not erase existing file!
)
@echo off
goto myloop

:myfin