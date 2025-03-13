import {FaCamera, FaSearch} from "react-icons/fa";
import {BiSolidMicrophone} from "react-icons/bi";

export function SearchBar({...props}) {
    return (<div {...props} className="flex justify-center w-full">
        <div
            className="relative flex items-center mx-2 h-[5vh] w-auto flex-grow max-w-[80vh] min-w-[30vh] rounded-2xl bg-white shadow-md">
            {/* Search Icon - Left */}
            <FaSearch className="absolute left-3 text-gray-500" size={13} />

            {/* Input Field */}
            <input
                className="w-full pl-10 pr-16 h-full rounded-2xl outline-none text-black bg-transparent font-medium text-xs"
            />

            {/* Right Icons - Microphone & Camera */}
            <div className="absolute right-3 flex space-x-2">
                <BiSolidMicrophone className="text-gray-500" />
                <FaCamera className="text-gray-500" />
            </div>
        </div>
    </div>);
}