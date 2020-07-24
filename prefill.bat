
@echo off
SET /A ARGS_COUNT=0
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1
@echo on
ECHO %ARGS_COUNT%
@echo off

SET /A mydate=%1

IF %ARGS_COUNT% == 0 (
    @echo on
    ECHO Missing arguments
    @echo off
) ELSE (
    IF %ARGS_COUNT% == 1 (
        for %%x in (facile, difficile, diabolique) do (
            copy sudoku_DDMM2020_TYPE.txt sudoku\sudoku_%12020_%%x.txt
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
copy sudoku_DDMM2020_TYPE.txt sudoku\sudoku_%mydate%2020_%1.txt
@echo off
goto myloop

:myfin