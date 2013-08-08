//Used to create the NATURALIZE CLR function
//adapted from:
//http://stackoverflow.com/questions/34509/natural-human-alpha-numeric-sort-in-microsoft-sql-2005
using System;
using System.Data.SqlTypes;
using System.Text;
using Microsoft.SqlServer.Server;

public class UDF
{
	[SqlFunction(DataAccess = DataAccessKind.None, IsDeterministic=true)]
    public static SqlString Naturalize(string val)
    {
        if (String.IsNullOrEmpty(val))
            return val;

        while(val.Contains("  "))
            val = val.Replace("  ", " ");

        const int maxLength = 1000;
        const int padLength = 25;

        bool inNumber = false;
        bool isDecimal = false;
        int numStart = 0;
        int numLength = 0;
        int length = val.Length < maxLength ? val.Length : maxLength;

        //TODO: optimize this so that we exit for loop once sb.ToString() >= maxLength
        var sb = new StringBuilder();
        for (var i = 0; i < length; i++)
        {
            int charCode = (int)val[i];
            if (charCode >= 48 && charCode <= 57)
            {
                if (!inNumber)
                {
                    numStart = i;
                    numLength = 1;
                    inNumber = true;
                    continue;
                }
                numLength++;
                continue;
            }
            if (inNumber)
            {
                sb.Append(PadNumber(val.Substring(numStart, numLength), isDecimal, padLength));
                inNumber = false;
            }
            isDecimal = (charCode == 46);
            sb.Append(val[i]);
        }
        if (inNumber)
            sb.Append(PadNumber(val.Substring(numStart, numLength), isDecimal, padLength));

        var ret = sb.ToString();
        if (ret.Length > maxLength)
            return ret.Substring(0, maxLength);

        return ret;
    }

    static string PadNumber(string num, bool isDecimal, int padLength)
    {
        return isDecimal ? num.PadRight(padLength, '0') : num.PadLeft(padLength, '0');
    }
}