package net.akaritakai.stream.net;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class DataUrlTest {

    @BeforeClass
    public static void setup() {
        DataUrlStreamHandlerFactory.register();
    }

    @Test
    public void basicTest0() throws IOException {
        DataUrlStreamHandlerFactory.register();

        URL url = new URL("data:,A%20brief%20note");

        URLConnection connection = url.openConnection();

        Assert.assertEquals("A brief note", IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));
    }

    @Test
    public void basicTest1() throws IOException {
        DataUrlStreamHandlerFactory.register();

        URL url = new URL("data:text/plain;charset=iso-8859-7,%be%fe%be");

        URLConnection connection = url.openConnection();

        Assert.assertEquals(3, IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8).length());
    }

    @Test
    public void imageTest() throws IOException {
        DataUrlStreamHandlerFactory.register();

        URL url = new URL("data:image/gif;base64,R0lGODlhQwBAAPcAAAAAAAAAMwAAZgAAmQAAzAAA/wArAAArMwArZgArmQArzAAr" +
                "/wBVAABVMwBVZgBVmQBVzABV/wCAAACAMwCAZgCAmQCAzACA/wCqAACqMwCqZgCqmQCqzACq/wDVAADVMwDVZgDVmQDVzADV/wD/AA" +
                "D/MwD/ZgD/mQD/zAD//zMAADMAMzMAZjMAmTMAzDMA/zMrADMrMzMrZjMrmTMrzDMr/zNVADNVMzNVZjNVmTNVzDNV/zOAADOAMzOA" +
                "ZjOAmTOAzDOA/zOqADOqMzOqZjOqmTOqzDOq/zPVADPVMzPVZjPVmTPVzDPV/zP/ADP/MzP/ZjP/mTP/zDP//2YAAGYAM2YAZmYAmW" +
                "YAzGYA/2YrAGYrM2YrZmYrmWYrzGYr/2ZVAGZVM2ZVZmZVmWZVzGZV/2aAAGaAM2aAZmaAmWaAzGaA/2aqAGaqM2aqZmaqmWaqzGaq" +
                "/2bVAGbVM2bVZmbVmWbVzGbV/2b/AGb/M2b/Zmb/mWb/zGb//5kAAJkAM5kAZpkAmZkAzJkA/5krAJkrM5krZpkrmZkrzJkr/5lVAJ" +
                "lVM5lVZplVmZlVzJlV/5mAAJmAM5mAZpmAmZmAzJmA/5mqAJmqM5mqZpmqmZmqzJmq/5nVAJnVM5nVZpnVmZnVzJnV/5n/AJn/M5n/" +
                "Zpn/mZn/zJn//8wAAMwAM8wAZswAmcwAzMwA/8wrAMwrM8wrZswrmcwrzMwr/8xVAMxVM8xVZsxVmcxVzMxV/8yAAMyAM8yAZsyAmc" +
                "yAzMyA/8yqAMyqM8yqZsyqmcyqzMyq/8zVAMzVM8zVZszVmczVzMzV/8z/AMz/M8z/Zsz/mcz/zMz///8AAP8AM/8AZv8Amf8AzP8A" +
                "//8rAP8rM/8rZv8rmf8rzP8r//9VAP9VM/9VZv9Vmf9VzP9V//+AAP+AM/+AZv+Amf+AzP+A//+qAP+qM/+qZv+qmf+qzP+q///VAP" +
                "/VM//VZv/Vmf/VzP/V////AP//M///Zv//mf//zP///wAAAAAAAAAAAAAAACH5BAEAAPwALAAAAABDAEAAAAj/AAEACAAgxkCDMQLE" +
                "WIhjYYwDCWEoLDgx4cMVEB86FIMDhwyHIEOGbBijYQAYB1YYwKgCIkQZN2J8jJjyAMoYBjSuxBkDo8OOYmJ2XDizpNGGSI8+HCjRZk" +
                "aGMhe6VGlR4UmFGZ8uFMMxRtcYMQ801EqybEkAKq8STBjTYc2WNFtKXEhRqlSgDbt+JCkzJkyjMLnckBHmAACIh2FolOoTocKVELHC" +
                "hdh468yOH9uKHEx0sGcZaCGrWGv14gGscw/IxRpjst2GfsOA/Xi5LwKwJW+IKUkYh4EAv1umzFlwZU6UcAvWjIHSZ87GRRsWbQtRc1" +
                "IcXJKiVW3YscW6Vpvn/yz9fLHbhdSjqt/LcAvsktkPH27Z8up45E6pPubpM6XGrAvdhlt16EGV12BI3VBQWggVp19EWGE0F2oWXSQS" +
                "SDPR1hdDn90gmAxiDAQZRZBhZN9Dcvlnk2k8/ecigCA1EBVJMNXo2VUFMVWRchg96GBKN61gnpAOFUWUjFG1BRthgXnUHXAqSQhcXJ" +
                "HdtByAlLm0WFZ8ZTbjbErhoCBLT07YVHIX5YRTj2qaB5JWIPmFm401hhHYYSieduZweUZU1UOK+aTQiyAJKMNL6GU2U4cy4HBSQjlC" +
                "eF94NSHHInMWhiSgQ5pxNptQXsF3Aw5NGYSRg8bR5FNdbaL01FRwHv8KVW2zeubhDQFkNN9payJm0U4rDseaiyFl1JasuJn1mZ1iyk" +
                "BVnhJKuZBEit3Q40KyPVUQSHzZ1RZninKGwxbL+qYQDmmtRK2Q1toUxm6YhsHIQkLCAC+maHArwxYZQtUXTOQ26hWCjeHa0go3qIDT" +
                "boq9G4bC70qCqVeSyAYWI5JssZUYFoMVMcd/geUewM0GhWdDEh4ghsLwOvzwDWHEDC/Gksy7hSS8MBKzbLrhdsMWYcCMrRiMTCLJGL" +
                "J5dCt2/ulmUwAqSIxbGBrvHDMMjcgWhiS7MLIIxrzgjDEjYtSjDAySRKL2GZJInAkajKDBVUl2hiFmdrga0OhHj4n/scUwytQDTSZh" +
                "RHKG4YajsQ89MWC8SNhht5OJMMOIkUw0+sSAyeaXCLM5DPrUs08mkmRC9Ni6jaoxRHYzJyTG+tADTT3DnCEMMpgg47kkyuyzCM5hSz" +
                "JMO/QQr8w8vdNzQzLJ5DMP88KIoU809ERTjz7DTIIJ2WJMspsYg3a0ghiZRM07PYIrE4Myz8zzDDSlL972MLxg4g7yydCTPzGhQ0NM" +
                "GMrQhwCTEYZ9LC4aixudMHqBiWEMQ25iUAZc/pIMBy5iEZRbRuiUEQZMVBATYqgePQA3D2LMQ3/FQyHy6EGPxaEhDHJDgzKUMTt9aF" +
                "AZ7SDGME74vzAQQxkp8UwM/4YRhmEsAhOTGEYmhjE7ehADE0vUhw6Hob924I8eyMuiMvQ3jB/SQx+9C90+zKYMwAGOhfTYYhg0Qb/f" +
                "lAQHOGsbI3bowHksUYlTHEYylJG/dmwxGcTLX/7ut8U/drFyDtSjOypIQhbmj4j0SCJGVEeMioWBF8er4DyomIznKWORKaygJIShwu" +
                "OpEIVpRF4Y0ohKLLJyi/SIoBgayKvdJOOCc1wkIZ+ny+dhMX+8CBsj/JhFUK6wlLEs5Qrzh8UtJlISvZhkQ+hRsyICco/EFOT9TDjC" +
                "OGLyhPkzJQ7FecIHsvCYzDweLGMpjEz0AicgGqIwdObAR/KyhL4UJBZr1v9J/d2vmSdc4QpJl8YtohONK8RELxyokFGJgRi3lMTvRo" +
                "i8dggykMSzaPE4qU6B/pGQWCSdIsN5UFhmj3IWwUFQLDjHHSaDkBrNnxZn6kp9MvOm3eTKGRixznA6MhO8GIbwTgKUGEwiDIuIBCaz" +
                "CdMR+jOLKORkCsHpSDSmE5I+bSY9FDqMXixxMSQhHcZcmk39ZTGLjwQoCtN6PHeoU6v0CANJ4So84WEyfOBjDs2oyMn78dWp+FQhKF" +
                "EZUFNSdYWr1KpBywjNTBAjEycbS0mGQbl54POlMu2jKTVL0z2i062o5GBHWVhIKm4xE/oIokqFpLKHLpGX/aRiPo1pWBP/Graji9Xq" +
                "KmGZzkKWUBPE8AlHDnAojsCMosczYSdNaEJwEtO24Bws8VApU2WIgarqnAQaJrFE4AIRYSrVSA44sgIcAjK5Mx3nYTe71lTiEIVuFW" +
                "pP6bFdNRDDgPj1yVBug4MxiEm5FMXnIq2oznbUMZGDxWduTZk9LdIjE5rQRBo0sQ8EGjAj4Y0BAsSQBo+8tq+d7OgiqcrJ6kL1pbed" +
                "RxLFOYwJa0INFMbvPuAyFIj01yNbwOw8RvzeHbYVs08FrG1rmkr6UhGF3I3whPehD/yG7yPE5bBHcKBcYoLyopnEoo9tykKp+lONb8" +
                "3EJCK8iSXXw8IGIK7THtIRjwQF/6IWjTNFaatR2Ta1usREIQyi6uIIw1jG+9DvbiAy3imnAQ3cxLIvd5joKrLXy/pUX5fJpwk4KDnG" +
                "CIxG+ExSEo4gYAxpGAMa9GhRTvoxpvgkpnmvzGoUaiEZxEDDG9QwCRe7mBjAJYZNGiUGiCBApTJolBrSwBWndlK2mOVxOZUZVVQikn" +
                "wTtnQmhm3fGYZCGSsIgG5IEgAx5KDNocZBGrjrR74q2LKr7meygaxFSYgBDeOe8JgtPQYKW3gfB7Bxrx8yBk9zeAw5GEM0sndsip5X" +
                "1ZtE7kxNSEeu9DcNb6AEHCAsYQqLzoDbqnFJ/FuSgPd3GTMkH0Rhy2Mfx3Sb+P/UYdt4kTQQqXQMY4BDvQ04vX3gNaVokMGGO8xr6c" +
                "QgDEms4jUJXGd/vnQY21u5JILtkUa1mSOUQODFJSIUl3DE51r6yKbGmlY6OpWv24tE29pmi7bF4EODYbrTD2jAXHVav07yNWuJqx4x" +
                "SHQLo6YiRB2YCZ39vGaSsIVS4xiUJjEJOw81oOgyHpYVqJTbdkmpSk8nG8XgBlMqkFfNghd4wLdQGZqgzZRhggNlEOPaqoXyxnGl4Y" +
                "TwLQbErnXhHNeangQqYp1/HCOCF7YfarBICOJ1wNOAp7nZOOeRsQtGRH3oMSTVFozQGQxgsIJ3Pe53j6tZJGg21htiyM2NqrXZGtjS" +
                "FX0HyPUJsUkajiYJNEg0qWNjxPa3D3jsh63swqPhDJcBFrqPXtyaMAkvERQXEV4qohGjwWHwxhU1wwhdA3/YJ3+6J0elU1D0sAyywy" +
                "nBFnxXty0ycRWDsRwCkH4KkQmNgAZj4G7W5zU18zsOGH0SBXiZwEoYOEOm12vBViNisDdDcRiskXzEpSVU8UPrF3sQJC/yojNx8zVk" +
                "MwmFFA0zBA0Y6D85BxgecRQykCuCYhFZIiSlQT6mh4ILqF3vFjd2d4ZpgAnEAA0z9EoFBXJW+C9TFgMBAQA7");

        URLConnection connection = url.openConnection();
        connection.connect();

        Assert.assertEquals(3324, connection.getContentLength());
        Assert.assertEquals("image/gif", connection.getContentType());

    }

}
